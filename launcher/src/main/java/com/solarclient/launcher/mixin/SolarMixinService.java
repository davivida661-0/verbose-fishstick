package com.solarclient.launcher.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <h1>Servico do Mixin para rodar sem Forge</h1>
 *
 * <p><b>Por que essa classe existe.</b> O Mixin nao funciona sozinho: quando
 * {@code MixinBootstrap.init()} roda, ele procura um {@link
 * org.spongepowered.asm.service.IMixinService} usando
 * {@link java.util.ServiceLoader}. Dentro do jar oficial do Mixin so vem o
 * registro para dois hospedeiros:</p>
 *
 * <ul>
 *     <li>{@code MixinServiceLaunchWrapperBootstrap} — so funciona com o
 *         <b>Forge</b> (LaunchWrapper);</li>
 *     <li>{@code MixinServiceModLauncherBootstrap} — so funciona com o
 *         <b>ModLauncher/Fabric</b>.</li>
 * </ul>
 *
 * <p>Como este projeto e justamente <b>sem Forge e sem Fabric</b>, nenhum dos
 * dois e valido e o {@code init()} estoura:</p>
 *
 * <pre>
 *   org.spongepowered.asm.service.ServiceNotAvailableError:
 *       No mixin host service is available.
 * </pre>
 *
 * <p>Por isso o jar do launcher carrega o registro
 * {@code META-INF/services/org.spongepowered.asm.service.IMixinService}
 * apontando para esta classe. Ela implementa o contrato minimo que o Mixin
 * precisa quando quem carrega as classes do jogo e <b>nos</b>
 * ({@link TransformingClassLoader}), e nao o Forge.</p>
 *
 * <p><b>Por que da para ser simples:</b> no Forge o servico tambem e quem
 * busca e transforma as classes. Aqui quem faz isso e o
 * {@link TransformingClassLoader}, entao os "providers" abaixo so precisam
 * entregar informacao — a transformacao de verdade acontece em
 * {@link TransformingClassLoader#findClass(String)}.</p>
 *
 * <p>Se em algum dia {@code init()} voltar a falhar com
 * {@code ServiceNotAvailableError}, quase sempre e porque o arquivo
 * {@code META-INF/services/...IMixinService} nao entrou no jar do launcher
 * (ele nao e gerado pelo compilador, entao o build precisa copia-lo de
 * {@code src/main/resources} — ver {@code launcher/build.gradle}).</p>
 */
public final class SolarMixinService extends MixinServiceAbstract {

    /** Provider de classes: delega para o classloader que esta rodando. */
    private final ClassProvider classProvider = new ClassProvider();

    /** Provider de bytecode: le bytes e converte em {@link ClassNode}. */
    private final BytecodeProvider bytecodeProvider = new BytecodeProvider();

    /** Providers auxiliares (transformers, tracker, audit trail). */
    private final TransformerProvider transformerProvider = new TransformerProvider();
    private final ClassTracker classTracker = new ClassTracker();
    private final AuditTrail auditTrail = new AuditTrail();

    /**
     * Container virtual que representa o launcher.
     *
     * <p>Nao pode ser {@code null}: o {@code MixinPlatformManager} desce nele
     * com {@code getNestedContainers()} logo no {@code init()} e estouraria
     * {@code NullPointerException}. Como nao existe jar de mod ainda nesse
     * ponto, um container virtual (sem arquivo) e a resposta certa.</p>
     */
    private final ContainerHandleVirtual primaryContainer =
            new ContainerHandleVirtual("solar-client-launcher");

    @Override
    public String getName() {
        return "Solar Client (standalone, sem Forge)";
    }

    @Override
    public boolean isValid() {
        // Sem hospedeiro: nao existe Forge nem ModLauncher para validar contra,
        // entao a unica condicao e estarmos dentro de uma JVM de verdade.
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return bytecodeProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return transformerProvider;
    }

    @Override
    public IClassTracker getClassTracker() {
        return classTracker;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return auditTrail;
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        // O container "primario" e um conceito do ModLauncher/Forge para
        // localizar o jar de onde veio a classe. Aqui quem localiza o jar e o
        // ClassLoader, mas o Mixin ainda exige um handle nao nulo, entao
        // devolvemos um container virtual.
        return primaryContainer;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        // Nenhum javaagent e instalado: a transformacao e feita pelo
        // TransformingClassLoader, nao por agente.
        return Collections.emptyList();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // Usado pelo Mixin para localizar o "solar.mixins.json" dentro do jar
        // do mod. O classloader de contexto e o do jogo, entao e ele quem sabe
        // onde esta o jar do mod.
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = SolarMixinService.class.getClassLoader();
        }
        InputStream stream = loader.getResourceAsStream(name);
        if (stream == null) {
            stream = SolarMixinService.class.getResourceAsStream(
                    name.startsWith("/") ? name : "/" + name);
        }
        return stream;
    }

    // ================================================================= providers

    /**
     * Resolve nomes de classe pelo ClassLoader do jogo.
     *
     * <p>E aqui que o Mixin "enxerga" o Minecraft: sem este provider ele nao
     * consegue ler as classes que vai transformar.</p>
     */
    static final class ClassProvider implements IClassProvider {

        @Override
        public URL[] getClassPath() {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            List<URL> urls = new ArrayList<>();
            while (loader != null) {
                if (loader instanceof java.net.URLClassLoader) {
                    Collections.addAll(urls, ((java.net.URLClassLoader) loader).getURLs());
                }
                loader = loader.getParent();
            }
            return urls.toArray(new URL[0]);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            return findClass(name, true);
        }

        @Override
        public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
            return Class.forName(name, initialize, gameLoader());
        }

        @Override
        public Class<?> findAgentClass(String name, boolean remap) throws ClassNotFoundException {
            // Nao ha agente: procuramos como classe normal.
            return findClass(name, false);
        }
    }

    /** Le os bytes de uma classe e converte para {@link ClassNode} com ASM. */
    static final class BytecodeProvider implements IClassBytecodeProvider {

        @Override
        public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
            return getClassNode(name, true);
        }

        @Override
        public ClassNode getClassNode(String name, boolean remap) throws ClassNotFoundException, IOException {
            byte[] bytes = read(name);
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            return node;
        }

        /** Le os bytes crus da classe sem instanciar (seguro p/ classe do jogo). */
        private byte[] read(String name) throws IOException {
            String path = name.replace('.', '/') + ".class";
            ClassLoader loader = gameLoader();
            try (InputStream in = loader.getResourceAsStream(path)) {
                if (in == null) {
                    return null;
                }
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            }
        }
    }

    /**
     * Nao ha transformadores registrados no servico.
     *
     * <p>A transformacao acontece no {@link TransformingClassLoader}. Esta lista
     * existe para o contrato do Mixin, que sempre pergunta quais transformers o
     * hospedeiro Declara — devolve vazio, nunca {@code null}.</p>
     */
    static final class TransformerProvider implements ITransformerProvider {

        private final Set<String> exclusions = new HashSet<>();

        @Override
        public Collection<ITransformer> getTransformers() {
            return Collections.emptyList();
        }

        @Override
        public Collection<ITransformer> getDelegatedTransformers() {
            return Collections.emptyList();
        }

        @Override
        public void addTransformerExclusion(String target) {
            exclusions.add(target);
        }
    }

    /**
     * Guarda quais classes ja foram transformadas.
     *
     * <p>Sem isso o Mixin poderia aplicar o mesmo mixin duas vezes na mesma
     * classe, o que corrompe o bytecode.</p>
     */
    static final class ClassTracker implements IClassTracker {

        private final Set<String> loaded = new HashSet<>();
        private final Set<String> invalid = new HashSet<>();
        private final Set<String> restricted = new HashSet<>();

        @Override
        public void registerInvalidClass(String className) {
            invalid.add(className);
        }

        @Override
        public boolean isClassLoaded(String className) {
            return loaded.contains(className);
        }

        @Override
        public String getClassRestrictions(String className) {
            return restricted.contains(className) ? "mixin.config.invalid" : null;
        }
    }

    /** Registro de auditoria do Mixin. Aqui so escreve log de verdade. */
    static final class AuditTrail implements IMixinAuditTrail {

        @Override
        public void onApply(String targetClassName, String mixinClassName) {
            log("aplicou " + mixinClassName + " em " + targetClassName);
        }

        @Override
        public void onPostProcess(String className) {
            log("pos-processou " + className);
        }

        @Override
        public void onGenerate(String className, String sourceClass) {
            log("gerou " + className + " a partir de " + sourceClass);
        }

        private void log(String message) {
            System.out.println("[mixin] " + message);
        }
    }

    // =================================================================== util

    /**
     * Descobre o ClassLoader que enxerga o jar do Minecraft.
     *
     * <p>Eh o classloader de contexto (o launcher define antes de chamar
     * {@code apply()}). Se nao houver nenhum, cai no classloader do proprio
     * launcher.</p>
     */
    private static ClassLoader gameLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader : SolarMixinService.class.getClassLoader();
    }

    /** Fase inicial: {@code PREINIT} e o padrao para mod em client-side. */
    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }
}
