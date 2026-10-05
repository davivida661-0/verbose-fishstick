package com.solarclient.launcher.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log simples do launcher (o Minecraft ainda nao existe nessa altura, entao nao
 * da para usar o log4j dele).
 */
public final class LauncherLog {

    private static final Logger LOG = Logger.getLogger("SolarLauncher");

    private LauncherLog() {
    }

    public static void info(String message) {
        LOG.info(message);
    }

    public static void warn(String message) {
        LOG.log(Level.WARNING, message);
    }

    public static void error(String message, Throwable t) {
        LOG.log(Level.SEVERE, message, t);
    }
}
