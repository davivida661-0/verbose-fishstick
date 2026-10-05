package com.solarclient.launcher.mixin;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * <h1>Bootstrap do Mixin</h1>
 *
 * <p>Sobe o ambiente do Mixin e registra o {@code solar.mixins.json} antes de
 * qualquer classe do Minecraft ser carregada.</p>
 *
 * <p>Ordem correta (a ordem importa - chamar {@code apply()} antes de
 * {@code init()} nao aplica nada):</p>
 * <pre>
 *   MixinBootstrapper.init();                                   // 1. sobe o Mixin
 *   ClassLoader game = new TransformingClassLoader(cp, parent); // 2. le as classes
 *   MixinBootstrapper.apply();                                  // 3. processa o JSON
 *   game.loadClass("net.minecraft.client.main.Main");           // 4. o jogo comeca
 * </pre>
 *
 * <p>Sobre a versao: o codigo usa a API do Mixin <b>0.8.5</b>
 * ({@code org.spongepowered.asm.launch.MixinBootstrap} e
 * {@code IMixinTransformer#transformClass}). Em versoes antigas (0.6.x) a
 * classe do bootstrap e {@code org.spongepowered.asm.mixin.MixinBootstrap} e a
 * interface do transformer era outra - se voce usar outra versao, ajuste
 * aqui e no {@link TransformingClassLoader}.</p>
 */
public final class MixinBootstrapper {

    private static final String CONFIG_NAME = "solar.mixins.json";

    private static boolean initialized;
    private static MixinEnvironment environment;

    private MixinBootstrapper() {
    }

    /**
     * Inicializa o ambiente do Mixin (precisa rodar uma vez, antes de carregar
     * qualquer classe do jogo).
     */
    public static void init() {
        if (initialized) {
            return;
        }
        MixinBootstrap.init();
        environment = MixinEnvironment.getCurrentEnvironment();
        initialized = true;
    }

    /**
     * Registra a configuracao de mixins do mod.
     *
     * <p>Chame <b>depois</b> de {@link #init()} e de dentro do
     * {@link TransformingClassLoader}, para que o classloader de mixins
     * enxergue o {@code solar.mixins.json} dentro do jar do mod.</p>
     */
    public static void apply(File modJar) {
        if (!initialized) {
            throw new IllegalStateException("Chame init() antes de apply()");
        }
        Mixins.addConfiguration(CONFIG_NAME);

        // MixinSo processa as configuracoes quando as classes comecam a ser
        // carregadas; getUnvisitedCount() != 0 aqui significa que o JSON nao
        // foi encontrado pelo classloader.
        int pending = Mixins.getUnvisitedCount();
        if (pending > 0) {
            throw new IllegalStateException(
                    "O Mixin nao achou " + CONFIG_NAME + " (pendentes: " + pending
                            + "). O jar do mod esta no classpath? " + modJar);
        }
    }

    public static MixinEnvironment getCurrentEnvironment() {
        if (!initialized) {
            throw new IllegalStateException("MixinBootstrapper.init() nao foi chamado");
        }
        return environment;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    /** Adiciona um jar ao classloader do sistema (o jar do mod precisa estar). */
    public static void addToSystemClassLoader(File jar) {
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (!(system instanceof URLClassLoader)) {
            return;
        }
        try {
            URLClassLoader urlClassLoader = (URLClassLoader) system;
            // URLClassLoader nao expe addURL: reflection e o caminho suportado
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(urlClassLoader, jar.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Jar invalido: " + jar, e);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel adicionar o jar ao classpath", e);
        }
    }
}
