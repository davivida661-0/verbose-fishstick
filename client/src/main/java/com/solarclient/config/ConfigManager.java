package com.solarclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.solarclient.SolarClient;
import com.solarclient.bedwars.SessionTracker;
import com.solarclient.mod.Mod;
import com.solarclient.mod.ModManager;
import com.solarclient.ui.Theme;
import com.solarclient.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import org.lwjgl.input.Keyboard;

/**
 * <h1>Configuracao em JSON</h1>
 *
 * <p>Um unico arquivo (<b>{@code solar-client/config.json}</b>) com tudo:
 * tema, estado de cada mod, posicao/escala do HUD, valores internos de cada
 * mod, ajustes do HUD Editor e os contadores de sessao.</p>
 *
 * <p>Estrutura do arquivo:</p>
 * <pre>
 * {
 *   "client":  { "name": "Solar Client", "version": "0.1.0", "accent": "PURPLE" },
 *   "hud":     { "snap": true, "grid": 5, "outline": 0.6 },
 *   "session": { "kills": 12, "beds": 4, "wins": 1 },
 *   "mods": {
 *     "Keystrokes": {
 *       "enabled": true, "x": 4, "y": 4, "scale": 1.0,
 *       "values": { "colorMode": 0, "cellSize": 26 }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Importante: o arquivo e lido <b>antes</b> dos mods serem registrados
 * (para a tecla de cada mod ja vir do disco) e escrito <b>depois</b> de tudo
 * (no shutdown e a cada alteracao no HUD Editor).</p>
 */
public final class ConfigManager {

    private static final String DIRECTORY = "solar-client";
    private static final String FILE = "config.json";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file = new File(DIRECTORY, FILE);

    private JsonObject root = new JsonObject();

    // ------------------------------------------------------------------ load
    public void load() {
        if (!file.exists()) {
            root = new JsonObject();
            Logger.info("Config novo criado em " + file.getAbsolutePath());
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), UTF8)) {
            JsonElement parsed = gson.fromJson(reader, JsonElement.class);
            root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            Logger.error("Falha ao ler o config (sera recriado): " + file, e);
            root = new JsonObject();
        }
        applyTheme();
    }

    /** Aplica tema/tamanho de HUD salvos (antes dos mods existirem). */
    private void applyTheme() {
        JsonObject hud = object("hud");
        if (hud.has("accent")) {
            String accent = hud.get("accent").getAsString();
            try {
                Theme.setPreset(Theme.Preset.valueOf(accent));
            } catch (IllegalArgumentException e) {
                Logger.warn("Tema desconhecido no config: " + accent + " (usando padrao)");
            }
        } else {
            Theme.setPreset(Theme.Preset.PURPLE);
        }
    }

    /**
     * Aplica o que esta salvo em cada mod: ligado/desligado, posicao, escala e
     * valores internos.
     */
    public void applyToMods(ModManager manager) {
        JsonObject mods = object("mods");

        // HUD Editor
        JsonObject hud = object("hud");
        snapEnabled = readBoolean(hud, "snap", true);
        snapGrid = readInt(hud, "grid", 5);

        // contadores de sessao
        JsonObject session = object("session");
        SessionTracker.get().load(
                readInt(session, "kills", 0),
                readInt(session, "beds", 0),
                readInt(session, "wins", 0));

        for (Mod mod : manager.all()) {
            JsonObject saved = mods.has(mod.getName()) ? mods.getAsJsonObject(mod.getName()) : null;
            if (saved == null) {
                continue; // mod novo: usa os defaults do construtor
            }
            mod.setPosition(readInt(saved, "x", mod.getX()), readInt(saved, "y", mod.getY()));
            mod.setScale((float) readDouble(saved, "scale", mod.getScale()));
            if (saved.has("values")) {
                mod.getValues().fromJson(saved.getAsJsonObject("values"));
            }
            // setEnabled por ultimo: assim onEnable ja ve os valores carregados
            mod.setEnabled(readBoolean(saved, "enabled", false));
        }
    }

    // ------------------------------------------------------------------ save
    public void save(ModManager manager) {
        JsonObject client = new JsonObject();
        client.addProperty("name", SolarClient.NAME);
        client.addProperty("version", SolarClient.VERSION);
        client.addProperty("accent", Theme.getPreset().name());
        root.add("client", client);

        JsonObject hud = new JsonObject();
        hud.addProperty("snap", snapEnabled);
        hud.addProperty("grid", snapGrid);
        root.add("hud", hud);

        JsonObject session = new JsonObject();
        session.addProperty("kills", SessionTracker.get().getKills());
        session.addProperty("beds", SessionTracker.get().getBeds());
        session.addProperty("wins", SessionTracker.get().getWins());
        root.add("session", session);

        // atalhos das telas (combos de 2 teclas)
        JsonObject gui = new JsonObject();
        gui.add("clickGuiKey", array(clickGuiKey));
        gui.add("hudEditorKey", array(hudEditorKey));
        gui.add("cosmeticsKey", array(cosmeticsKey));
        root.add("gui", gui);

        JsonObject mods = new JsonObject();
        for (Mod mod : manager.all()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enabled", mod.isEnabled());
            entry.addProperty("x", mod.getX());
            entry.addProperty("y", mod.getY());
            entry.addProperty("scale", mod.getScale());
            entry.add("values", mod.getValues().toJson());
            mods.add(mod.getName(), entry);
        }
        root.add("mods", mods);

        write();
    }

    /** Salva sem passar pelo ModManager (usado pelo HUD Editor a cada arraste). */
    public void save() {
        write();
    }

    private void write() {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                Logger.warn("Nao foi possivel criar a pasta " + dir);
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), UTF8)) {
                gson.toJson(root, writer);
            }
        } catch (IOException e) {
            Logger.error("Falha ao salvar o config", e);
        }
    }

    // ------------------------------------------------------------------ HUD
    private boolean snapEnabled = true;
    private int snapGrid = 5;

    public boolean isSnapEnabled() {
        return snapEnabled;
    }

    public void setSnapEnabled(boolean snapEnabled) {
        this.snapEnabled = snapEnabled;
    }

    public int getSnapGrid() {
        return snapGrid;
    }

    public void setSnapGrid(int snapGrid) {
        this.snapGrid = Math.max(1, snapGrid);
    }

    /** Troca o tema e ja persiste. */
    public void setAccent(Theme.Preset preset) {
        Theme.setPreset(preset);
        object("hud").addProperty("accent", preset.name());
        write();
    }

    // ------------------------------------------------------------------ atalhos
    // Combos de 2 teclas que abrem as telas do client (usados pelo GuiShortcuts).
    private int[] clickGuiKey = {Keyboard.KEY_R, Keyboard.KEY_S};
    private int[] hudEditorKey = {Keyboard.KEY_H, Keyboard.KEY_U};
    private int[] cosmeticsKey = {Keyboard.KEY_C, Keyboard.KEY_O};

    public void setChord(String key, int[] keys) {
        if (keys == null || keys.length != 2) {
            return;
        }
        switch (key) {
            case "clickGuiKey":
                clickGuiKey = keys.clone();
                break;
            case "hudEditorKey":
                hudEditorKey = keys.clone();
                break;
            case "cosmeticsKey":
                cosmeticsKey = keys.clone();
                break;
            default:
                break;
        }
        object("gui").add(key, array(keys));
    }

    // ------------------------------------------------------------------ utils
    private static com.google.gson.JsonArray array(int[] keys) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (int key : keys) {
            array.add(key);
        }
        return array;
    }

    private JsonObject object(String name) {
        if (!root.has(name) || !root.get(name).isJsonObject()) {
            root.add(name, new JsonObject());
        }
        return root.getAsJsonObject(name);
    }

    private static boolean readBoolean(JsonObject json, String key, boolean def) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsBoolean() : def;
    }

    private static int readInt(JsonObject json, String key, int def) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : def;
    }

    private static double readDouble(JsonObject json, String key, double def) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : def;
    }

    /** Caminho absoluto do arquivo (mostrado no ClickGUI). */
    public String getPath() {
        return file.getAbsolutePath();
    }

    /**
     * Combo de teclas salvo no config (usado pelo {@code GuiShortcuts}).
     *
     * @return array de 2 keycodes
     */
    public int[] getChord(String key) {
        JsonObject gui = root.has("gui") && root.get("gui").isJsonObject()
                ? root.getAsJsonObject("gui") : new JsonObject();
        if (gui.has(key) && gui.get(key).isJsonArray()) {
            com.google.gson.JsonArray array = gui.getAsJsonArray(key);
            if (array.size() == 2) {
                int[] keys = {array.get(0).getAsInt(), array.get(1).getAsInt()};
                switch (key) {
                    case "clickGuiKey":
                        clickGuiKey = keys;
                        break;
                    case "hudEditorKey":
                        hudEditorKey = keys;
                        break;
                    case "cosmeticsKey":
                        cosmeticsKey = keys;
                        break;
                    default:
                        break;
                }
                return keys;
            }
        }
        switch (key) {
            case "clickGuiKey":
                return clickGuiKey;
            case "hudEditorKey":
                return hudEditorKey;
            case "cosmeticsKey":
                return cosmeticsKey;
            default:
                return null;
        }
    }
}
