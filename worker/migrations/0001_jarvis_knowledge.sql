CREATE TABLE IF NOT EXISTS learning_sources (
  id TEXT PRIMARY KEY, url TEXT NOT NULL UNIQUE, title TEXT, domain TEXT,
  discovered_at TEXT NOT NULL, checked_at TEXT, trust_tier TEXT NOT NULL DEFAULT 'unreviewed'
);
CREATE TABLE IF NOT EXISTS knowledge_items (
  id TEXT PRIMARY KEY, topic TEXT NOT NULL, claim TEXT NOT NULL, summary TEXT NOT NULL,
  source_url TEXT NOT NULL, source_title TEXT, published_at TEXT, learned_at TEXT NOT NULL,
  confidence REAL NOT NULL DEFAULT 0.5,
  verification_status TEXT NOT NULL DEFAULT 'unverified',
  content_hash TEXT NOT NULL UNIQUE, expires_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_knowledge_topic ON knowledge_items(topic, learned_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_expiry ON knowledge_items(expires_at);
CREATE TABLE IF NOT EXISTS learning_runs (
  id TEXT PRIMARY KEY, started_at TEXT NOT NULL, finished_at TEXT, status TEXT NOT NULL,
  sources_scanned INTEGER NOT NULL DEFAULT 0, items_saved INTEGER NOT NULL DEFAULT 0, error TEXT
);
CREATE TABLE IF NOT EXISTS learning_preferences (
  key TEXT PRIMARY KEY, value TEXT NOT NULL, updated_at TEXT NOT NULL
);
