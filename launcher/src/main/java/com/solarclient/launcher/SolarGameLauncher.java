package com.solarclient.launcher;

import com.solarclient.launcher.auth.AuthManager;
import com.solarclient.launcher.download.Downloader;
import com.solarclient.launcher.mixin.MixinBootstrapper;
import com.solarclient.launcher.mixin.TransformingClassLoader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>SolarGameLauncher</h1>
 *
 * <p>Monta o classpath, registra o Mixin e chama o {@code main} do Minecraft
 * 1.8.9 <b>dentro do processo do launcher</b> (sem {@code java -jar}, sem
 * Forge, sem agent).</p>
 *
 * <p>Ordem exata:</p>
 * <ol>
 *     <li>baixa/valida o jar do jogo, as libraries, os assets e o OptiFine;</li>
 *     <li>poe o jar do Solar Client e o do OptiFine no classpath;</li>
 *     <li>{@link MixinBootstrapper#init(File)} + {@link TransformingClassLoader};</li>
 *     <li>{@code net.minecraft.client.main.Main.main(args)} com os argumentos
 *         padrao do vanilla mais o {@code --accessToken}/{@code --uuid} da
 *         sessao;</li>
 *     <li>apos o jogo subir, um hook chama
 *         {@code SolarClient.get().init()} (o mixin do Minecraft tambem chama
 *         no primeiro tick, entao isso e apenas um atalho).</li>
 * </ol>
 */
public final class SolarGameLauncher {

    private static final String MC_MAIN = "net.minecraft.client.main.Main";

    private final File gameDir;
    private final File cache;
    private final File modJar;
    private final String version;
    private final boolean optiFine;

    public SolarGameLauncher(File gameDir, File cache, File modJar, String version, boolean optiFine) {
        this.gameDir = gameDir;
        this.cache = cache;
        this.modJar = modJar;
        this.version = version;
        this.optiFine = optiFine;
    }

    /**
     * Baixa tudo e inicia o jogo.
     *
     * @param session sessao do login (pode ser offline)
     */
    public void launch(AuthManager.Session session) throws Exception {
        List<File> classpath = new ArrayList<>();

        // 1) jogo
        Downloader downloader = new Downloader(cache, version, (message, percent) ->
                System.out.println("[" + percent + "%] " + message));
        classpath.addAll(downloader.prepare(new ArrayList<>()));

        // 2) mod do client (vem no classpath para o Mixin achar o JSON)
        classpath.add(modJar);

        // 3) OptiFine
        if (optiFine) {
            File installer = new File(cache, "OptiFine_1.8.9_HD_U_M5.jar");
            if (!installer.exists()) {
                System.out.println("[optifine] baixe o instalador em https://optifine.net/download "
                        + "e salve em " + installer.getAbsolutePath());
            } else {
                classpath.add(downloader.installOptiFine(installer, classpath.get(0)));
            }
        }

        // 4) URLs do classpath
        List<URL> urls = new ArrayList<>();
        for (File file : classpath) {
            urls.add(file.toURI().toURL());
        }

        // 5) Mixin
        //    a ordem importa: o jar do mod vai para o classloader do sistema,
        //    o ambiente do Mixin sobe antes de qualquer classe do jogo, e o
        //    apply() roda com o TransformingClassLoader ja como contexto (e ele
        //    que consegue ler o solar.mixins.json de dentro do jar do mod).
        MixinBootstrapper.addToSystemClassLoader(modJar);
        MixinBootstrapper.init();

        TransformingClassLoader loader = new TransformingClassLoader(urls,
                ClassLoader.getSystemClassLoader().getParent());
        Thread.currentThread().setContextClassLoader(loader);
        MixinBootstrapper.apply(modJar);

        // 6) argumentos do vanilla
        String[] args = buildArgs(session);

        // 7) start
        Class<?> main = loader.loadClass(MC_MAIN);
        Method mainMethod = main.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

    private String[] buildArgs(AuthManager.Session session) {
        File assets = new File(gameDir, "assets");
        return new String[]{
                "--username", session.username,
                "--version", version,
                "--gameDir", gameDir.getAbsolutePath(),
                "--assetsDir", assets.getAbsolutePath(),
                "--assetIndex", "1.8",
                "--uuid", session.profileId(),
                "--accessToken", session.accessToken,
                "--userProperties", "{}",
                "--userType", session.clientId == null ? "legacy" : "msa",
        };
    }
}
