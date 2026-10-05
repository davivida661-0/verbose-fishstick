package com.solarclient.cosmetics;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solarclient.util.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h1>Cliente da API de cosmeticos</h1>
 *
 * <p>Fala com a API REST usando apenas o {@code HttpURLConnection} do JDK
 * (zero library extra no mod). Todas as chamadas rodam em uma thread daemon
 * para nunca travar o jogo - o resultado volta por callback e quem aplica
 * no jogo e o {@link CosmeticManager}.</p>
 *
 * <p>Endpoints (documentacao completa em {@code docs/API-COSMETICS.md}):</p>
 * <pre>
 *   GET  {base}/v1/cosmetics/loadout?uuid=...   -> { "CAPE": "solar_cape" }
 *   POST {base}/v1/loadout                      body: { "uuid": "...", "slots": {...} }
 *   GET  {base}/v1/cosmetics/catalog            -> catalogo com raridades
 * </pre>
 *
 * <p>A URL base vem de {@code gradle.properties} ({@code apiBaseUrl}) e pode
 * ser trocada em runtime pela aba Settings do ClickGUI.</p>
 */
public final class CosmeticsApi {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int TIMEOUT_MS = 8000;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Solar-Api");
        thread.setDaemon(true);
        return thread;
    });

    private final Gson gson = new Gson();
    private volatile String baseUrl = "https://api.solarclient.dev";
    private volatile String apiKey = "";

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    // ------------------------------------------------------------------ GET
    /** Busca o que o jogador tem equipado no servidor. */
    public void fetchLoadout(UUID uuid, java.util.function.Consumer<Map<CosmeticType, String>> callback) {
        POOL.submit(() -> {
            try {
                String url = baseUrl + "/v1/cosmetics/loadout?uuid=" + uuid;
                String body = get(url);
                if (body == null) {
                    return;
                }
                JsonObject json = new JsonParser().parse(body).getAsJsonObject();
                JsonObject slots = json.has("slots") && json.get("slots").isJsonObject()
                        ? json.getAsJsonObject("slots") : json;

                Map<CosmeticType, String> loadout = new HashMap<>();
                for (String key : slots.keySet()) {
                    try {
                        loadout.put(CosmeticType.valueOf(key), slots.get(key).getAsString());
                    } catch (IllegalArgumentException ignored) {
                        // tipo desconhecido no servidor: ignora
                    }
                }
                callback.accept(loadout);
            } catch (Exception e) {
                Logger.warn("API indisponivel ao buscar o loadout: " + e.getMessage());
            }
        });
    }

    /** Baixa o catalogo (id, tipo, raridade). */
    public void fetchCatalog(java.util.function.Consumer<Map<String, Rarity>> callback) {
        POOL.submit(() -> {
            try {
                String body = get(baseUrl + "/v1/cosmetics/catalog");
                if (body == null) {
                    return;
                }
                Map<String, Rarity> result = new HashMap<>();
                JsonElement parsed = new JsonParser().parse(body);
                JsonArrayLike array = new JsonArrayLike(parsed);
                for (int i = 0; i < array.size(); i++) {
                    JsonObject item = array.get(i);
                    result.put(item.get("id").getAsString(),
                            Rarity.fromName(item.has("rarity") ? item.get("rarity").getAsString() : null));
                }
                callback.accept(result);
            } catch (Exception e) {
                Logger.warn("API indisponivel ao buscar o catalogo: " + e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------ POST
    /** Salva o equipado no servidor. */
    public void saveLoadout(UUID uuid, Map<CosmeticType, String> slots, Runnable onSuccess) {
        POOL.submit(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("uuid", uuid.toString());
                JsonObject jsonSlots = new JsonObject();
                for (Map.Entry<CosmeticType, String> slot : slots.entrySet()) {
                    jsonSlots.addProperty(slot.getKey().name(), slot.getValue());
                }
                payload.add("slots", jsonSlots);

                HttpURLConnection connection = open(baseUrl + "/v1/loadout");
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                byte[] data = gson.toJson(payload).getBytes(UTF8);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(data);
                }
                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    onSuccess.run();
                } else {
                    Logger.warn("API respondeu " + code + " ao salvar o loadout");
                }
            } catch (Exception e) {
                Logger.warn("API indisponivel ao salvar o loadout: " + e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------ http
    private String get(String url) throws Exception {
        HttpURLConnection connection = open(url);
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        try (InputStream in = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    private HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "SolarClient/1.0");
        if (!apiKey.isEmpty()) {
            connection.setRequestProperty("X-Api-Key", apiKey);
        }
        return connection;
    }

    /** Wrapper minimo para ler um array JSON sem trazer uma library. */
    private static final class JsonArrayLike {
        private final com.google.gson.JsonArray array;

        private JsonArrayLike(JsonElement element) {
            this.array = element.isJsonArray() ? element.getAsJsonArray() : new com.google.gson.JsonArray();
        }

        private int size() {
            return array.size();
        }

        private JsonObject get(int index) {
            return array.get(index).getAsJsonObject();
        }
    }

    /** URL encode simples (usado no query string). */
    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
