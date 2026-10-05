package com.solarclient.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>API REST de cosmeticos</h1>
 *
 * <p>Servidor HTTP minimo, sem framework: usa o {@code com.sun.net.httpserver}
 * que ja vem no Java 8. Zero dependencia de aplicacao - o unico extra e o
 * driver do SQLite.</p>
 *
 * <p>Rode com:</p>
 * <pre>
 *   gradle :api:run
 *   # ou, com o jar:
 *   java -cp "api/build/libs/*:gson.jar:sqlite-jdbc.jar" com.solarclient.api.CosmeticsApiServer
 * </pre>
 *
 * <p>Porta: {@code -Dsolar.api.port=8787} (padrao 8787).</p>
 *
 * <p>Autenticacao: header {@code X-Api-Key} igual a variavel de ambiente
 * {@code SOLAR_API_KEY}. Se a variavel nao estiver setada, a API sobe aberta -
 * perfeito para desenvolvimento local, nunca para producao.</p>
 *
 * <p>Rotas (ver {@code docs/API-COSMETICS.md}):</p>
 * <pre>
 *   GET  /v1/health
 *   GET  /v1/cosmetics/catalog
 *   GET  /v1/cosmetics/owned?uuid=...
 *   GET  /v1/cosmetics/loadout?uuid=...
 *   POST /v1/loadout
 *   POST /v1/admin/unlock   (admin)
 * </pre>
 */
public final class CosmeticsApiServer {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_BODY = 64 * 1024;

    private final Database database;
    private final String apiKey;
    private final Map<String, Object[]> routes = new HashMap<>();

    private HttpServer server;

    public CosmeticsApiServer() {
        this.apiKey = System.getenv("SOLAR_API_KEY");
        this.database = new Database(new File("data", "cosmetics.db"));
        registerRoutes();
    }

    // ------------------------------------------------------------------ rotas
    private void registerRoutes() {
        routes.put("GET /v1/health", new Object[]{"health", (Route) exchange -> {
            Json.write(exchange, 200, "{\"status\":\"ok\"}");
        }});

        routes.put("GET /v1/cosmetics/catalog", new Object[]{"catalog",
                (Route) exchange -> Json.write(exchange, 200, database.catalog())});

        routes.put("GET /v1/cosmetics/owned", new Object[]{"owned",
                (Route) exchange -> {
                    String uuid = exchange.getRequestURI().getQuery();
                    uuid = query(uuid, "uuid");
                    Json.write(exchange, 200, database.owned(uuid));
                }});

        routes.put("GET /v1/cosmetics/loadout", new Object[]{"loadout",
                (Route) exchange -> {
                    String uuid = query(exchange.getRequestURI().getQuery(), "uuid");
                    Json.write(exchange, 200, database.loadout(uuid));
                }});

        routes.put("POST /v1/loadout", new Object[]{"loadout",
                (Route) exchange -> {
                    String body = readBody(exchange);
                    if (!Json.isAuthorized(exchange, apiKey)) {
                        Json.write(exchange, 401, "{\"error\":\"unauthorized\"}");
                        return;
                    }
                    database.saveLoadout(body);
                    Json.write(exchange, 200, "{\"ok\":true}");
                }});

        routes.put("POST /v1/admin/unlock", new Object[]{"unlock",
                (Route) exchange -> {
                    if (!Json.isAuthorized(exchange, apiKey)) {
                        Json.write(exchange, 401, "{\"error\":\"unauthorized\"}");
                        return;
                    }
                    database.unlock(readBody(exchange));
                    Json.write(exchange, 200, "{\"ok\":true}");
                }});
    }

    // ------------------------------------------------------------------ server
    public void start(int port) throws IOException {
        database.migrate();
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // qualquer rota nao mapeada cai aqui
        server.createContext("/", exchange -> {
            String key = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
            Object[] route = routes.get(key);
            if (route == null) {
                Json.write(exchange, 404, "{\"error\":\"not found\",\"path\":\"" + key + "\"}");
                return;
            }
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            try {
                ((Route) route[1]).handle(exchange);
            } catch (Exception e) {
                Json.write(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Solar Cosmetics API rodando em http://localhost:" + port);
        System.out.println("Banco: " + new File("data", "cosmetics.db").getAbsolutePath());
        System.out.println(apiKey == null
                ? "ATENCAO: SOLAR_API_KEY nao definida - API sem autenticacao (apenas dev!)"
                : "Autenticacao por X-Api-Key ativa.");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        database.close();
    }

    // ------------------------------------------------------------------ utils
    private static String query(String query, String key) {
        if (query == null) {
            return "";
        }
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals(key)) {
                return pair[1];
            }
        }
        return "";
    }

    static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] data = new byte[Math.min(MAX_BODY, in.available())];
            in.read(data);
            return new String(data, UTF8);
        }
    }

    static void write(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(UTF8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String escape(String text) {
        return text == null ? "erro" : text.replace('"', '\'');
    }

    /** Handler de uma rota. */
    private interface Route {
        void handle(HttpExchange exchange) throws IOException;
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("solar.api.port",
                System.getenv().getOrDefault("SOLAR_API_PORT", "8787")));
        CosmeticsApiServer server = new CosmeticsApiServer();
        server.start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
