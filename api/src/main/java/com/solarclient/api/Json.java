package com.solarclient.api;

import com.sun.net.httpserver.HttpExchange;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Helpers de JSON e autorizacao da API.
 *
 * <p>A construcao de JSON usa o Gson (ja e dependencia do modulo); aqui ficam
 * so as duas coisinhas de HTTP que toda rota precisa.</p>
 */
public final class Json {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Json() {
    }

    /** Escreve a resposta JSON. */
    static void write(HttpExchange exchange, int code, String body) throws java.io.IOException {
        CosmeticsApiServer.write(exchange, code, body);
    }

    /**
     * Confere o header {@code X-Api-Key}. Se {@code expected} for null, a API
     * esta sem autenticacao (modo de desenvolvimento).
     */
    static boolean isAuthorized(HttpExchange exchange, String expected) {
        if (expected == null || expected.isEmpty()) {
            return true;
        }
        String provided = exchange.getRequestHeaders().getFirst("X-Api-Key");
        return provided != null && constantTimeEquals(expected, provided);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] left = a.getBytes(UTF8);
        byte[] right = b.getBytes(UTF8);
        return MessageDigest.isEqual(left, right);
    }
}
