package com.solarclient.launcher.mixin;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.File;
import java.lang.reflect.Constructor;
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

    /**
     * Transformer criado pelo launcher (ver {@link #ensureTransformer()}).
     *
     * <p>Guardado so para diagnostico: o {@code TransformingClassLoader} le o
     * transformer direto do {@link MixinEnvironment}.</p>
     */
    private static IMixinTransformer transformer;

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
    }    /**
     * Cria o transformer do Mixin e registra no {@link MixinEnvironment}.
     *
     * <p><b>Por que isso e necessario aqui.</b> Quem cria o
     * {@code MixinTransformer} normalmente e o hospedeiro: no Forge, o
     * {@code Proxy} do proprio Mixin (que implementa a interface do
     * LaunchWrapper); no Fabric, o ModLauncher. Nao existe caminho publico
     * "comum" — {@code Proxy} nem carrega sem o Forge, porque a classe
     * implementa {@code net.minecraft.launchwrapper.IClassTransformer}.</p>
     *
     * <p>Como este launcher <b>nao tem Forge nem Fabric</b>, a unica saida e
     * construir o transformer pela {@code MixinTransformer.Factory} por
     * reflexao. Ela e package-private, mas e a fabrica que o proprio Mixin usa,
     * e o metodo {@code createTransformer()} ja faz o
     * {@code setActiveTransformer} no ambiente. Sem este passo,
     * {@code getActiveTransformer()} devolve {@code null} e o
     * {@link TransformingClassLoader} cai silenciosamente nos bytes originais —
     * ou seja, o mod inteiro nao faz nada, sem erro nenhum.</p>
     *
     * <p><b>Por que isso mora em {@link #apply(File)} e nao em {@link #init()}.</b>
     * O construtor do {@code MixinTransformer} le a lista de alvos das
     * configuracoes <b>na hora em que e criado</b>. Se ele nascesse antes do
     * {@code addConfiguration}, ele guardaria uma lista de alvos vazia para
     * sempre e nenhuma classe seria transformada — sem erro, sem aviso. No
     * Forge a ordem não é problema porque o launcher carrega os mods (e
     * registra as configs) antes de o transformer ser criado.</p>
     *
     * <p>Se este codigo deixar de funcionar numa versao nova do Mixin, o
     * sintoma e o mod carregar sem aplicar nada. Nesse caso, veja qual classe
     * substitui {@code MixinTransformer$Factory} na versao nova.</p>
     */
    private static void ensureTransformer() {
        if (environment.getActiveTransformer() instanceof IMixinTransformer) {
            return;
        }
        try {
            Class<?> factoryClass =
                    Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer$Factory");
            Constructor<?> constructor = factoryClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object factory = constructor.newInstance();

            Method createTransformer = factoryClass.getMethod("createTransformer");
            createTransformer.setAccessible(true);
            transformer = (IMixinTransformer) createTransformer.invoke(factory);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Nao foi possivel criar o transformer do Mixin. Confirme a versao "
                            + "do Mixin em gradle.properties (mixinVersion) e as libs ASM "
                            + "no classpath: asm, asm-tree, asm-util, asm-commons e "
                            + "asm-analysis sao obrigatorias em runtime.", e);
        }
        if (!(environment.getActiveTransformer() instanceof IMixinTransformer)) {
            throw new IllegalStateException(
                    "O transformer do Mixin foi criado mas nao foi registrado no ambiente.");
        }
    }

    /** Transformer ativo, ou {@code null} se o Mixin ainda nao subiu. */
    public static IMixinTransformer getTransformer() {
        return transformer;
    }

    /**
     * Registra a configuracao de mixins do mod.
     *
     * <p>Chame <b>depois</b> de {@link #init()} e de dentro do
     * {@link TransformingClassLoader}, para que o classloader de mixins
     * enxergue o {@code solar.mixins.json} dentro do jar do mod.</p>
     *
     * <p><b>Nao</b> use {@code Mixins.getUnvisitedCount()} para checar se o
     * JSON foi encontrado: essa contagem so zera quando o Mixin <i>seleciona</i>
     * a configuracao, o que acontece na primeira carga de classe do jogo. Logo
     * apos {@code addConfiguration} ela e sempre maior que zero, mesmo com tudo
     * certo. O proprio {@code addConfiguration} ja estoura
     * {@code MixinInitialisationError} se o recurso nao existir; aqui a gente so
     * troca essa excecao por uma mensagem que diz o que fazer.</p>
     */
    public static void apply(File modJar) {
        if (!initialized) {
            throw new IllegalStateException("Chame init() antes de apply()");
        }
        try {
            Mixins.addConfiguration(CONFIG_NAME);
            // DEPOIS de registrar a config: o transformer le os alvos na criacao.
            ensureTransformer();
        } catch (Throwable t) {
            throw new IllegalStateException(
                    "O Mixin nao leu " + CONFIG_NAME + " (" + t.getMessage()
                            + "). O jar do mod esta no classpath? " + modJar, t);
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
