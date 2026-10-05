package com.solarclient.launcher.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <h1>Login Microsoft / Mojang</h1>
 *
 * <p>Usa o <b>OAuth Device Code</b> da Microsoft: o jogo mostra um codigo de
 * 8 caracteres, o usuario abre a pagina que o proprio jogo imprime, digita o
 * codigo e autoriza. Nao existe segredo no launcher (nenhum client_id
 * secreto), que e exatamente o motivo de esse fluxo ser o recomendado.</p>
 *
 * <p>Para um launcher publico voce precisa registrar um aplicativo em
 * <a href="https://portal.azure.com/">Azure Portal</a> e colocar o
 * {@code clientId} em {@code gradle.properties}
 * ({@code microsoftClientId}). Sem ele, so o modo offline funciona.</p>
 *
 * <p>Fluxo resumido:</p>
 * <ol>
 *   <li>{@code POST https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode}</li>
 *   <li>o jogo mostra {@code user_code} + {@code verification_uri}</li>
 *   <li>{@code POST .../token} em loop ate o usuario aprovar (ou expirar)</li>
 *   <li>com o {@code refresh_token}, troca por um token da Xbox Live
 *       (XBL3.0 x=uhs;token)</li>
 *   <li>depois XSTS e, por fim, o token do Minecraft
 *       (Yggdrasil com o hash do Xbox).</li>
 * </ol>
 */
public final class AuthManager {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String DEVICE_CODE_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String SCOPE = "XboxLive.signin offline_access";

    /** Dados da sessao usados para iniciar o jogo. */
    public static final class Session {
        public String username;
        public String uuid;       // sem traco, como o jogo espera
        public String accessToken;
        public String clientId;

        public String profileId() {
            return "00000000-0000-0000-0000-" + pad(uuid);
        }

        private static String pad(String hex) {
            StringBuilder sb = new StringBuilder();
            for (int i = hex.length(); i < 12; i++) {
                sb.append('0');
            }
            return sb + hex;
        }
    }

    private final String clientId;

    public AuthManager(String clientId) {
        this.clientId = clientId;
    }

    // ------------------------------------------------------------------ offline
    /** Modo offline: util para teste e para quem nao tem conta. */
    public Session offline(String username) {
        Session session = new Session();
        session.username = username;
        session.accessToken = "0";
        session.clientId = clientId;
        // UUID v3 deterministico a partir do nome (o proprio jogo faz igual)
        session.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(UTF8))
                .toString().replace("-", "");
        return session;
    }

    // ------------------------------------------------------------------ device
    /**
     * Inicia o fluxo. Chame em uma thread separada: ele fica esperando o
     * usuario aprovar no navegador.
     *
     * @param listener recebe as informacoes do codigo para mostrar no jogo
     */
    public Session loginWithDeviceCode(DeviceCodeListener listener) throws Exception {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("scope", SCOPE);

        JsonObject device = JsonParser.parseString(post(DEVICE_CODE_URL, form, true))
                .getAsJsonObject();
        listener.onCode(device.get("user_code").getAsString(),
                device.get("verification_uri").getAsString(),
                device.get("interval").getAsInt(),
                device.get("expires_in").getAsInt());

        Map<String, String> tokenForm = new LinkedHashMap<>();
        tokenForm.put("client_id", clientId);
        tokenForm.put("scope", SCOPE);
        tokenForm.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        tokenForm.put("device_code", device.get("device_code").getAsString());

        int interval = device.get("interval").getAsInt() * 1000;
        long deadline = System.currentTimeMillis() + device.get("expires_in").getAsLong() * 1000L;
        String token = null;

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval);
            try {
                token = post(TOKEN_URL, tokenForm, true);
                break;
            } catch (Exception e) {
                // "authorization_pending" / "slow_down": apenas tenta de novo
                listener.onWaiting();
            }
        }
        if (token == null) {
            throw new IllegalStateException("Login cancelado ou expirado.");
        }

        String refreshToken = JsonParser.parseString(token).getAsJsonObject()
                .get("refresh_token").getAsString();

        return exchangeForMinecraft(refreshToken, listener);
    }

    /** Troca o refresh token pelos tokens do Xbox Live -> XSTS -> Minecraft. */
    private Session exchangeForMinecraft(String refreshToken, DeviceCodeListener listener)
            throws Exception {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("scope", SCOPE);
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        String tokens = post(TOKEN_URL, form, true);
        String access = JsonParser.parseString(tokens).getAsJsonObject()
                .get("access_token").getAsString();

        // 1) Xbox Live
        JsonObject xbl = postJsonObject("https://user.auth.xboxlive.com/user/authenticate",
                "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\","
                        + "\"RpsTicket\":\"" + access + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\","
                        + "\"TokenType\":\"JWT\"}", "application/json", null);
        String xblToken = xbl.getAsJsonObject("Token").get("Token").getAsString();
        String userHash = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                .get(0).getAsJsonObject().get("uhs").getAsString();

        // 2) XSTS
        JsonObject xsts = postJsonObject("https://xsts.auth.xboxlive.com/xsts/authorize",
                "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken
                        + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\","
                        + "\"TokenType\":\"JWT\"}", "application/json", null);
        String xstsToken = xsts.getAsJsonObject("Token").get("Token").getAsString();

        // 3) Minecraft (Yggdrasil)
        JsonObject yggdrasil = postJsonObject("https://api.minecraftservices.com/authentication/login_with_xbox",
                "{\"identityToken\":\"XBL3.0 x=" + userHash + ";" + xstsToken + "\"}",
                "application/json", "Bearer " + access);
        String mcToken = yggdrasil.getAsJsonObject("access_token").getAsString();

        JsonObject profile = getJsonObject("https://api.minecraftservices.com/minecraft/profile",
                "Bearer " + mcToken);
        listener.onSuccess(profile.get("name").getAsString());

        Session session = new Session();
        session.username = profile.get("name").getAsString();
        session.uuid = profile.get("id").getAsString();
        session.accessToken = mcToken;
        session.clientId = clientId;
        return session;
    }

    // ------------------------------------------------------------------ http
    private static String post(String url, Map<String, String> form, boolean urlEncoded)
            throws Exception {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return postJson(url, body.toString(), "application/x-www-form-urlencoded", null);
    }

    private static String postJson(String url, String json, String contentType, String bearer)
            throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Accept", "application/json");
        if (bearer != null) {
            connection.setRequestProperty("Authorization", bearer);
        }
        try (OutputStream out = connection.getOutputStream()) {
            out.write(json.getBytes(UTF8));
        }
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code + " em " + url);
        }
        return read(connection.getInputStream());
    }

    private static String getJson(String url, String bearer) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Accept", "application/json");
        if (bearer != null) {
            connection.setRequestProperty("Authorization", bearer);
        }
        return read(connection.getInputStream());
    }

    private static JsonObject postJsonObject(String url, String json, String contentType,
                                             String bearer) throws Exception {
        return JsonParser.parseString(postJson(url, json, contentType, bearer)).getAsJsonObject();
    }

    private static JsonObject getJsonObject(String url, String bearer) throws Exception {
        return JsonParser.parseString(getJson(url, bearer)).getAsJsonObject();
    }

    private static String read(InputStream in) throws Exception {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    /** Callbacks da tela de login. */
    public interface DeviceCodeListener {
        /** Mostra o codigo e a URL para o usuario. */
        void onCode(String userCode, String verificationUri, int intervalSeconds, int expiresSeconds);

        /** Ainda esperando o usuario autorizar. */
        void onWaiting();

        void onSuccess(String username);
    }
}
