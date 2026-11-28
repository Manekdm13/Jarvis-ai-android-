export interface LearningDb {
  prepare(sql: string): { bind(...values: unknown[]): { run(): Promise<unknown>; all<T = unknown>(): Promise<{ results: T[] }> } };
}
export type LearningSummary = { topic: string; claim: string; summary: string; confidence: number };

const FEEDS = [
  { url: "https://export.arxiv.org/rss/cs.AI", topic: "artificial intelligence" },
  { url: "https://export.arxiv.org/rss/cs.LG", topic: "machine learning" },
  { url: "https://export.arxiv.org/rss/q-bio", topic: "biology and computational biology" },
  { url: "https://export.arxiv.org/rss/physics", topic: "physics" },
  { url: "https://www.nasa.gov/feed/", topic: "space science" },
];

function safeText(value: string, max: number): string {
  return value.replace(/<[^>]*>/g, " ").replace(/&nbsp;/g, " ").replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&#39;/g, "'")
    .replace(/&quot;/g, '"').replace(/\s+/g, " ").trim().slice(0, max);
}
function tag(xml: string, name: string): string {
  const match = xml.match(new RegExp("<" + name + "(?:\\s[^>]*)?>([\\s\\S]*?)</" + name + ">", "i"));
  return match?.[1]?.replace(/^<!\[CDATA\[/, "").replace(/\]\]>$/, "") || "";
}
function hashText(value: string): string {
  let hash = 2166136261;
  for (let i = 0; i < value.length; i++) hash = Math.imul(hash ^ value.charCodeAt(i), 16777619);
  return (hash >>> 0).toString(16).padStart(8, "0");
}

/** Scheduled, bounded discovery. Retrieved pages are untrusted data and never executable instructions. */
export async function runLearningCycle(
  db: LearningDb,
  summarize: (topic: string, title: string, url: string, excerpt: string) => Promise<LearningSummary>,
): Promise<{ scanned: number; saved: number; errors: string[] }> {
  const runId = crypto.randomUUID();
  const started = new Date().toISOString();
  await db.prepare("INSERT INTO learning_runs (id, started_at, status) VALUES (?, ?, 'running')").bind(runId, started).run();
  let scanned = 0;
  let saved = 0;
  const errors: string[] = [];
  for (const feed of FEEDS) {
    try {
      const response = await fetch(feed.url, { headers: { "Accept": "application/rss+xml, application/atom+xml, application/xml, text/xml" }, signal: AbortSignal.timeout(12000) });
      if (!response.ok) throw new Error("HTTP " + response.status);
      const xml = (await response.text()).slice(0, 1_000_000);
      const entries = xml.match(/<(?:item|entry)(?:\s[^>]*)?>[\s\S]*?<\/(?:item|entry)>/gi) || [];
      for (const entry of entries.slice(0, 3)) {
        const title = safeText(tag(entry, "title"), 240);
        const link = safeText(tag(entry, "link"), 1000) || safeText(entry.match(/<link[^>]+href=["']([^"']+)/i)?.[1] || "", 1000);
        const description = safeText(tag(entry, "description") || tag(entry, "summary") || tag(entry, "content"), 4500);
        const published = safeText(tag(entry, "pubDate") || tag(entry, "published") || tag(entry, "updated"), 80);
        if (!title || !/^https:\/\//i.test(link)) continue;
        scanned++;
        const sourceId = hashText(link);
        await db.prepare("INSERT OR IGNORE INTO learning_sources (id,url,title,domain,discovered_at,checked_at,trust_tier) VALUES (?,?,?,?,?,?,?)")
          .bind(sourceId, link, title, new URL(link).hostname, started, started, "publisher-feed").run();
        const digest = hashText((title + " " + description).toLowerCase());
        const exists = await db.prepare("SELECT id FROM knowledge_items WHERE content_hash = ? LIMIT 1").bind(digest).all<{id:string}>();
        if (exists.results.length) continue;
        const item = await summarize(feed.topic, title, link, description);
        const confidence = Math.max(0, Math.min(0.75, Number(item.confidence) || 0.5));
        await db.prepare("INSERT OR IGNORE INTO knowledge_items (id,topic,claim,summary,source_url,source_title,published_at,learned_at,confidence,verification_status,content_hash,expires_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")
          .bind(crypto.randomUUID(), safeText(item.topic || feed.topic, 120), safeText(item.claim, 700),
            safeText(item.summary, 2200), link, title, published || null, started, confidence,
            "single-source-needs-corroboration", digest, null).run();
        saved++;
      }
    } catch (error) {
      errors.push(feed.url + ": " + (error instanceof Error ? error.message : "feed failed"));
    }
  }
  await db.prepare("UPDATE learning_runs SET finished_at=?, status=?, sources_scanned=?, items_saved=?, error=? WHERE id=?")
    .bind(new Date().toISOString(), errors.length ? (saved ? "partial" : "failed") : "completed", scanned, saved, errors.join("; ").slice(0, 1500) || null, runId).run();
  return { scanned, saved, errors };
}

export async function learningStatus(db: LearningDb): Promise<unknown> {
  const [runs, count] = await Promise.all([
    db.prepare("SELECT id,started_at,finished_at,status,sources_scanned,items_saved,error FROM learning_runs ORDER BY started_at DESC LIMIT 10").bind().all(),
    db.prepare("SELECT COUNT(*) AS total FROM knowledge_items").bind().all<{total:number}>(),
  ]);
  return { totalKnowledgeItems: count.results[0]?.total || 0, recentRuns: runs.results };
}

export async function searchKnowledge(db: LearningDb, query: string): Promise<unknown[]> {
  const term = "%" + query.replace(/[\%_]/g, "").slice(0, 100) + "%";
  const result = await db.prepare("SELECT topic,claim,summary,source_url,source_title,published_at,learned_at,confidence,verification_status FROM knowledge_items WHERE topic LIKE ? OR claim LIKE ? OR summary LIKE ? ORDER BY learned_at DESC LIMIT 20")
    .bind(term, term, term).all();
  return result.results;
}
