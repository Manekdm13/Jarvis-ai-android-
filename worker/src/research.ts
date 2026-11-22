export type ResearchPaper = {
  source: "Semantic Scholar" | "OpenAlex";
  title: string;
  year: number | null;
  authors: string[];
  venue: string | null;
  doi: string | null;
  abstract: string | null;
  citations: number | null;
  url: string | null;
  openAccessUrl: string | null;
};

type ResearchEnv = {
  SEMANTIC_SCHOLAR_API_KEY?: string;
  OPENALEX_API_KEY?: string;
};

function text(value: unknown, max = 1200): string | null {
  return typeof value === "string" && value.trim() ? value.trim().slice(0, max) : null;
}

function openAlexAbstract(inverted: Record<string, number[]> | null | undefined): string | null {
  if (!inverted || typeof inverted !== "object") return null;
  const words: string[] = [];
  for (const [word, positions] of Object.entries(inverted)) {
    if (!Array.isArray(positions)) continue;
    for (const position of positions) {
      if (Number.isInteger(position) && position >= 0 && position < 100_000) words[position] = word;
    }
  }
  return words.filter(Boolean).join(" ").slice(0, 1200) || null;
}

async function fetchJson(url: URL, headers: HeadersInit = {}): Promise<any> {
  const response = await fetch(url, { headers: { Accept: "application/json", ...headers } });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

async function semanticScholar(query: string, limit: number, env: ResearchEnv): Promise<ResearchPaper[]> {
  const url = new URL("https://api.semanticscholar.org/graph/v1/paper/search");
  url.searchParams.set("query", query);
  url.searchParams.set("limit", String(limit));
  url.searchParams.set("fields", "title,abstract,year,authors,venue,externalIds,url,citationCount,openAccessPdf");
  const headers: Record<string, string> = {};
  if (env.SEMANTIC_SCHOLAR_API_KEY) headers["x-api-key"] = env.SEMANTIC_SCHOLAR_API_KEY;
  const data = await fetchJson(url, headers);
  return (Array.isArray(data?.data) ? data.data : []).map((paper: any): ResearchPaper => ({
    source: "Semantic Scholar",
    title: text(paper.title, 500) || "Untitled paper",
    year: Number.isInteger(paper.year) ? paper.year : null,
    authors: Array.isArray(paper.authors) ? paper.authors.slice(0, 6).map((a: any) => text(a?.name, 120)).filter(Boolean) as string[] : [],
    venue: text(paper.venue, 200),
    doi: text(paper.externalIds?.DOI, 200),
    abstract: text(paper.abstract),
    citations: Number.isFinite(paper.citationCount) ? paper.citationCount : null,
    url: text(paper.url, 1000),
    openAccessUrl: text(paper.openAccessPdf?.url, 1000),
  }));
}

async function openAlex(query: string, limit: number, env: ResearchEnv): Promise<ResearchPaper[]> {
  const url = new URL("https://api.openalex.org/works");
  url.searchParams.set("search", query);
  url.searchParams.set("per-page", String(limit));
  url.searchParams.set("select", "id,doi,title,publication_year,authorships,primary_location,biblio,cited_by_count,open_access,abstract_inverted_index");
  if (env.OPENALEX_API_KEY) url.searchParams.set("api_key", env.OPENALEX_API_KEY);
  const data = await fetchJson(url);
  return (Array.isArray(data?.results) ? data.results : []).map((paper: any): ResearchPaper => {
    const authors = Array.isArray(paper.authorships)
      ? paper.authorships.slice(0, 6).map((a: any) => text(a?.author?.display_name, 120)).filter(Boolean) as string[]
      : [];
    const doi = text(paper.doi, 250)?.replace(/^https?:\/\/doi\.org\//i, "") || null;
    return {
      source: "OpenAlex",
      title: text(paper.title, 500) || "Untitled paper",
      year: Number.isInteger(paper.publication_year) ? paper.publication_year : null,
      authors,
      venue: text(paper.primary_location?.source?.display_name, 200),
      doi,
      abstract: openAlexAbstract(paper.abstract_inverted_index),
      citations: Number.isFinite(paper.cited_by_count) ? paper.cited_by_count : null,
      url: text(paper.id, 1000),
      openAccessUrl: text(paper.open_access?.oa_url, 1000),
    };
  });
}

export async function searchAcademicPapers(queryInput: unknown, limitInput: unknown, env: ResearchEnv) {
  const query = text(queryInput, 300);
  if (!query || query.length < 3) throw new Error("Enter a research topic of at least 3 characters.");
  const limit = Number.isInteger(limitInput) ? Math.max(1, Math.min(8, Number(limitInput))) : 5;

  const [semantic, openalex] = await Promise.allSettled([
    semanticScholar(query, limit, env),
    openAlex(query, limit, env),
  ]);
  const papers = [
    ...(semantic.status === "fulfilled" ? semantic.value : []),
    ...(openalex.status === "fulfilled" ? openalex.value : []),
  ];
  const seen = new Set<string>();
  const unique = papers.filter((paper) => {
    const key = (paper.doi || paper.title).toLowerCase().replace(/[^a-z0-9]/g, "");
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  }).slice(0, 12);

  const errors = [
    ...(semantic.status === "rejected" ? [`Semantic Scholar: ${String(semantic.reason?.message || semantic.reason).slice(0, 180)}`] : []),
    ...(openalex.status === "rejected" ? [`OpenAlex: ${String(openalex.reason?.message || openalex.reason).slice(0, 180)}`] : []),
  ];
  if (unique.length === 0) {
    throw new Error(errors.length ? `Both research sources failed. ${errors.join("; ")}` : "No papers were found. Try broader keywords.");
  }
  return { query, papers: unique, sources: ["Semantic Scholar", "OpenAlex"], partial: errors.length > 0, warnings: errors };
}
