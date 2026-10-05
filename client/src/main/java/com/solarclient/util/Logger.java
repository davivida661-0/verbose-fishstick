package com.solarclient.util;

/**
 * Wrapper fino sobre o log4j que ja vem no Minecraft 1.8.9.
 *
 * <p>O prefixo de categoria e sempre "SolarClient", assim da para filtrar tudo
 * que o client escreveu no log do jogo com {@code grep SolarClient latest.log}.</p>
 */
public final class Logger {

    private static final org.apache.logging.log4j.Logger LOG =
            org.apache.logging.log4j.LogManager.getLogger("SolarClient");

    private Logger() {
    }

    public static void info(String msg) {
        LOG.info(msg);
    }

    public static void warn(String msg) {
        LOG.warn(msg);
    }

    public static void error(String msg) {
        LOG.error(msg);
    }

    public static void error(String msg, Throwable t) {
        LOG.error(msg, t);
    }

    public static void debug(String msg) {
        LOG.debug(msg);
    }
}
