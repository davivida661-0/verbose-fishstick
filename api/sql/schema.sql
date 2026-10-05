-- ---------------------------------------------------------------------------
-- Schema da API de cosmeticos do Solar Client
--
-- SQLite (padrao, zero setup):
--   sqlite3 data/cosmetics.db < sql/schema.sql
--
-- Postgres (multi-servidor): os tipos mudam, o resto e igual.
--   CREATE TABLE ... UUID / TEXT / BIGINT / TIMESTAMPTZ
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS cosmetics (
    id           TEXT PRIMARY KEY,          -- 'solar_cape'
    type         TEXT NOT NULL,             -- CAPE | CLOAK | DRAGON_WINGS | ...
    rarity       TEXT NOT NULL,             -- COMMON | RARE | EPIC | LEGENDARY
    display_name TEXT NOT NULL,             -- 'Capa Solar'
    texture      TEXT                       -- 'cosmetics/capes/solar_cape.png'
);

-- O que o jogador desbloqueou (por compra, evento, nivel, etc).
CREATE TABLE IF NOT EXISTS ownership (
    uuid         TEXT NOT NULL,             -- uuid SEM traco (32 chars)
    cosmetic_id  TEXT NOT NULL,
    unlocked_at  INTEGER NOT NULL,          -- epoch millis
    PRIMARY KEY (uuid, cosmetic_id),
    FOREIGN KEY (cosmetic_id) REFERENCES cosmetics(id) ON DELETE CASCADE
);

-- O que esta equipado agora (so um por tipo).
CREATE TABLE IF NOT EXISTS loadout (
    uuid         TEXT NOT NULL,
    type         TEXT NOT NULL,
    cosmetic_id  TEXT,
    PRIMARY KEY (uuid, type),
    FOREIGN KEY (cosmetic_id) REFERENCES cosmetics(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ownership_uuid ON ownership (uuid);
CREATE INDEX IF NOT EXISTS idx_loadout_uuid ON loadout (uuid);

-- Catalogo inicial (o mesmo que o mod conhece).
INSERT OR IGNORE INTO cosmetics VALUES
    ('solar_cape', 'CAPE', 'LEGENDARY', 'Capa Solar',  'cosmetics/capes/solar_cape.png'),
    ('ember_cape', 'CAPE', 'EPIC',      'Capa Brasa',  'cosmetics/capes/ember_cape.png'),
    ('frost_cape', 'CAPE', 'RARE',      'Capa Gelo',   'cosmetics/capes/frost_cape.png');
