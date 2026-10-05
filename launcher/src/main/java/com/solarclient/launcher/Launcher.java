package com.solarclient.launcher;

import com.solarclient.launcher.auth.AuthManager;
import com.solarclient.launcher.download.Downloader;

import java.io.Console;
import java.io.File;
import java.util.Properties;

/**
 * <h1>Solar Client Launcher</h1>
 *
 * <p>Main do launcher. Sem interface grafica Java: o fluxo e por linha de
 * comando (e o shell Electron em {@code electron-shell/} serve como casca
 * visual para quem quiser).</p>
 *
 * <pre>
 *   java -jar solar-client-launcher.jar --user NomeDoJogador
 *   java -jar solar-client-launcher.jar --user Nome --online
 *   java -jar solar-client-launcher.jar --user Nome --no-optifine
 * </pre>
 *
 * <p>Argumentos:</p>
 * <ul>
 *     <li>{@code --user <nome>} obrigatorio;</li>
 *     <li>{@code --online} usa o login Microsoft (device code);</li>
 *     <li>{@code --no-optifine} roda so com o OptiFine desativado;</li>
 *     <li>{@code --version <1.8.9>} padrao 1.8.9;</li>
 *     <li>{@code --game-dir <pasta>} padrao {@code .}</li>
 * </ul>
 */
public final class Launcher {

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        Properties properties = loadProperties();

        String clientId = properties.getProperty("microsoftClientId", "");
        AuthManager auth = new AuthManager(clientId);
        AuthManager.Session session;

        if (options.online) {
            if (clientId.isEmpty()) {
                System.err.println("Falta microsoftClientId em gradle.properties "
                        + "(registre um app em https://portal.azure.com/).");
                System.exit(1);
            }
            session = loginMicrosoft(auth);
        } else {
            System.out.println("Modo offline como '" + options.username + "'.");
            session = auth.offline(options.username);
        }

        System.out.println("Jogando como " + session.username + " (" + session.uuid + ")");

        File gameDir = new File(options.gameDir);
        if (!gameDir.exists() && !gameDir.mkdirs()) {
            System.err.println("Nao foi possivel criar a pasta do jogo: " + gameDir);
            System.exit(1);
        }
        File cache = new File("cache");
        File modJar = findModJar();

        if (modJar == null) {
            System.err.println("Jar do mod nao encontrado. Compile antes: gradle :client:jar");
            System.exit(1);
        }

        new SolarGameLauncher(gameDir, cache, modJar, options.version, !options.noOptiFine)
                .launch(session);
    }

    private static AuthManager.Session loginMicrosoft(AuthManager auth) {
        Console console = System.console();
        final AuthManager.Session[] result = new AuthManager.Session[1];
        Runnable login = () -> {
            try {
                result[0] = auth.loginWithDeviceCode(new AuthManager.DeviceCodeListener() {
                    @Override
                    public void onCode(String userCode, String uri, int interval, int expires) {
                        System.out.println();
                        System.out.println("  Entre em " + uri);
                        System.out.println("  e digite o codigo: " + userCode);
                        System.out.println("  (valido por " + expires + "s)");
                        if (console != null) {
                            console.readLine();
                        }
                    }

                    @Override
                    public void onWaiting() {
                        System.out.print(".");
                    }

                    @Override
                    public void onSuccess(String username) {
                        System.out.println("\nBem-vindo, " + username + "!");
                    }
                });
            } catch (Exception e) {
                System.err.println("Falha no login: " + e.getMessage());
            }
        };

        Thread thread = new Thread(login, "Solar-Login");
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (result[0] == null) {
            System.exit(1);
        }
        return result[0];
    }

    /** Procura o jar do mod no classpath ou nas pastas de build. */
    private static File findModJar() {
        for (String candidate : new String[]{
                "client/build/libs/solar-client-" + version() + ".jar",
                "build/libs/solar-client-" + version() + ".jar",
                "solar-client.jar"}) {
            File file = new File(candidate);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    private static String version() {
        return System.getProperty("solar.version", "0.1.0");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        File file = new File("gradle.properties");
        if (file.exists()) {
            try (java.io.FileReader reader = new java.io.FileReader(file)) {
                properties.load(reader);
            } catch (Exception e) {
                System.err.println("Nao foi possivel ler gradle.properties: " + e.getMessage());
            }
        }
        return properties;
    }

    /** Argumentos de linha de comando. */
    static final class Options {
        String username = "Player";
        boolean online;
        boolean noOptiFine;
        String version = "1.8.9";
        String gameDir = ".";

        static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--user":
                        options.username = args[++i];
                        break;
                    case "--online":
                        options.online = true;
                        break;
                    case "--no-optifine":
                        options.noOptiFine = true;
                        break;
                    case "--version":
                        options.version = args[++i];
                        break;
                    case "--game-dir":
                        options.gameDir = args[++i];
                        break;
                    default:
                        System.err.println("Argumento desconhecido: " + args[i]);
                }
            }
            return options;
        }
    }

    /** Callback simples de progresso reusado pelo console. */
    static final class ConsoleProgress implements Downloader.ProgressListener {
        @Override
        public void onProgress(String message, int percent) {
            System.out.println("[" + percent + "%] " + message);
        }
    }
}
