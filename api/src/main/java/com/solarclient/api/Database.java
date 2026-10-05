package com.solarclient.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * <h1>Banco (SQLite)</h1>
 *
 * <p>Tres tabelas:</p>
 * <pre>
 *   cosmetics(id, type, rarity, display_name, texture)      -- catalogo
 *   ownership(uuid, cosmetic_id, unlocked_at)               -- o que o jogo liberou
 *   loadout(uuid, type, cosmetic_id)                        -- o que esta equipado
 * </pre>
 *
 * <p>Se quiser Postgres em vez de SQLite (multi-servidor, por exemplo), o
 * unico lugar que muda e este arquivo: as consultas sao quase todas ANSI.
 * O schema equivalente esta em {@code api/sql/schema.sql}.</p>
 */
public final class Database {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final File file;
    private Connection connection;

    public Database(File file) {
        this.file = file;
    }

    // ------------------------------------------------------------------ setup
    public void migrate() {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Nao foi possivel criar " + dir);
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS cosmetics ("
                        + "id TEXT PRIMARY KEY, type TEXT NOT NULL, rarity TEXT NOT NULL, "
                        + "display_name TEXT NOT NULL, texture TEXT)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS ownership ("
                        + "uuid TEXT NOT NULL, cosmetic_id TEXT NOT NULL, "
                        + "unlocked_at INTEGER NOT NULL, "
                        + "PRIMARY KEY (uuid, cosmetic_id))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS loadout ("
                        + "uuid TEXT NOT NULL, type TEXT NOT NULL, cosmetic_id TEXT, "
                        + "PRIMARY KEY (uuid, type))");
                seed(st);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao iniciar o banco", e);
        }
    }

    /** Catalogo inicial, igual ao que vem no mod. */
    private void seed(Statement st) throws SQLException {
        st.executeUpdate("INSERT OR IGNORE INTO cosmetics VALUES "
                + "('solar_cape','CAPE','LEGENDARY','Capa Solar','cosmetics/capes/solar_cape.png')");
        st.executeUpdate("INSERT OR IGNORE INTO cosmetics VALUES "
                + "('ember_cape','CAPE','EPIC','Capa Brasa','cosmetics/capes/ember_cape.png')");
        st.executeUpdate("INSERT OR IGNORE INTO cosmetics VALUES "
                + "('frost_cape','CAPE','RARE','Capa Gelo','cosmetics/capes/frost_cape.png')");
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // fechando mesmo assim
            }
        }
    }

    // ------------------------------------------------------------------ queries
    public String catalog() {
        JsonArray array = new JsonArray();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, type, rarity, display_name, texture FROM cosmetics ORDER BY type, id")) {
            while (rs.next()) {
                JsonObject item = new JsonObject();
                item.addProperty("id", rs.getString("id"));
                item.addProperty("type", rs.getString("type"));
                item.addProperty("rarity", rs.getString("rarity"));
                item.addProperty("name", rs.getString("display_name"));
                item.addProperty("texture", rs.getString("texture"));
                array.add(item);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao ler o catalogo", e);
        }
        return array.toString();
    }

    public String owned(String uuid) {
        JsonArray array = new JsonArray();
        if (uuid == null || uuid.isEmpty()) {
            return array.toString();
        }
        String sql = "SELECT c.id, c.rarity, c.type FROM ownership o "
                + "JOIN cosmetics c ON c.id = o.cosmetic_id WHERE o.uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalize(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("id", rs.getString("id"));
                    item.addProperty("rarity", rs.getString("rarity"));
                    item.addProperty("type", rs.getString("type"));
                    array.add(item);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao ler os cosmeticos do jogador", e);
        }
        return array.toString();
    }

    public String loadout(String uuid) {
        JsonObject root = new JsonObject();
        root.addProperty("uuid", uuid == null ? "" : uuid);
        JsonObject slots = new JsonObject();
        if (uuid != null && !uuid.isEmpty()) {
            String sql = "SELECT type, cosmetic_id FROM loadout WHERE uuid = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, normalize(uuid));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("cosmetic_id");
                        if (id != null) {
                            slots.addProperty(rs.getString("type"), id);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Falha ao ler o loadout", e);
            }
        }
        root.add("slots", slots);
        return root.toString();
    }

    /** Salva o equipado. Body: {@code {"uuid":"...","slots":{"CAPE":"solar_cape"}}}. */
    public void saveLoadout(String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String uuid = normalize(json.get("uuid").getAsString());
        JsonObject slots = json.getAsJsonObject("slots");

        String delete = "DELETE FROM loadout WHERE uuid = ?";
        String insert = "INSERT INTO loadout (uuid, type, cosmetic_id) VALUES (?, ?, ?)";
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(delete)) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(insert)) {
                for (String type : slots.keySet()) {
                    ps.setString(1, uuid);
                    ps.setString(2, type);
                    ps.setString(3, slots.get(type).getAsString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
        } catch (Exception e) {
            rollback();
            throw new IllegalStateException("Falha ao salvar o loadout", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // segue a vida
            }
        }
    }

    /** Libera um cosmetico para um jogador. Body: {@code {"uuid":"...","cosmeticId":"..."}}. */
    public void unlock(String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String uuid = normalize(json.get("uuid").getAsString());
        String cosmeticId = json.get("cosmeticId").getAsString();
        String sql = "INSERT OR IGNORE INTO ownership (uuid, cosmetic_id, unlocked_at) "
                + "VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, cosmeticId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao liberar o cosmetico", e);
        }
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // nada a fazer
        }
    }

    /** UUIDs vem com e sem traco do cliente: normaliza. */
    private static String normalize(String uuid) {
        if (uuid == null) {
            return "";
        }
        String clean = uuid.trim().toLowerCase();
        if (clean.length() == 32) {
            return clean;
        }
        try {
            return UUID.fromString(clean).toString();
        } catch (IllegalArgumentException e) {
            return clean;
        }
    }
}
