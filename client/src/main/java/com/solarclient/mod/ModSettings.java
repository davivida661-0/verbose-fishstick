package com.solarclient.mod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Configuracoes internas de um mod (booleans, numeros, cores...).
 *
 * <p>Foi desenhada assim de proposito: em vez de uma classe de Settings por mod
 * (que duplicaria dezenas de getters), cada mod le e escreve por chave. O
 * objeto inteiro e serializado direto no {@code config.json}, entao da para
 * criar um valor novo sem migrar schema nenhum.</p>
 *
 * <pre>{@code
 * boolean rainbow = values.getBoolean("rainbow", false);
 * int blur = values.getInt("blur", 4);
 * int color = values.getColor("color", 0xFF7C5CFF);
 * }</pre>
 */
public final class ModSettings {

    private final JsonObject json = new JsonObject();

    public JsonObject toJson() {
        return json;
    }

    public void fromJson(JsonObject other) {
        json.entrySet().clear();
        if (other != null) {
            for (Map.Entry<String, JsonElement> e : other.entrySet()) {
                json.add(e.getKey(), e.getValue());
            }
        }
    }

    public void copyFrom(ModSettings other) {
        fromJson(other.json);
    }

    public boolean has(String key) {
        return json.has(key);
    }

    public void remove(String key) {
        json.remove(key);
    }

    // ------------------------------------------------------------------ boolean
    public boolean getBoolean(String key, boolean def) {
        JsonElement e = json.get(key);
        return e == null || e.isJsonNull() ? def : e.getAsBoolean();
    }

    public void set(String key, boolean value) {
        json.addProperty(key, value);
    }

    // ------------------------------------------------------------------ int
    public int getInt(String key, int def) {
        JsonElement e = json.get(key);
        return e == null || e.isJsonNull() ? def : e.getAsInt();
    }

    public void set(String key, int value) {
        json.addProperty(key, value);
    }

    // ------------------------------------------------------------------ double
    public double getDouble(String key, double def) {
        JsonElement e = json.get(key);
        return e == null || e.isJsonNull() ? def : e.getAsDouble();
    }

    public void set(String key, double value) {
        json.addProperty(key, value);
    }

    // ------------------------------------------------------------------ string
    public String getString(String key, String def) {
        JsonElement e = json.get(key);
        return e == null || e.isJsonNull() ? def : e.getAsString();
    }

    public void set(String key, String value) {
        json.addProperty(key, value);
    }

    // ------------------------------------------------------------------ cor ARGB
    public int getColor(String key, int def) {
        String hex = getString(key, null);
        if (hex == null) {
            return def;
        }
        try {
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void setColor(String key, int argb) {
        json.addProperty(key, String.format("%08X", argb));
    }
}
