package com.solarclient.launcher.mixin;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * <h1>ClassLoader que aplica os mixins</h1>
 *
 * <p>Este e o coracao do "client sem Forge": o jar do Minecraft continua
 * exatamente como a Mojang publicou, e o Mixin reescreve os bytes das classes
 * <b>em memoria</b> no momento em que elas sao carregadas.</p>
 *
 * <p>Como funciona:</p>
 * <ol>
 *     <li>o launcher cria este classloader com o classpath do jogo;</li>
 *     <li>{@link MixinBootstrapper#init()} sobe o ambiente do Mixin e
 *         {@code apply()} registra o {@code solar.mixins.json};</li>
 *     <li>aqui, ao procurar uma classe, os bytes originais sao lidos do jar e
 *         passam por {@link IMixinTransformer#transformClass} - que devolve a
 *         classe ja com as injecoes;</li>
 *     <li>o resto do jogo roda normalmente: por fora parece "vanilla".</li>
 * </ol>
 *
 * <p>Por que nao um javaagent? Os dois funcionam; o classloader e o caminho
 * mais simples de controlar porque o launcher ja cria o processo do jogo.</p>
 *
 * <p><b>Ordem:</b> {@code MixinBootstrapper.apply()} tem que rodar DENTRO
 * deste classloader (por isso ele e chamado depois que o loader existe) - e o
 * que faz o {@code solar.mixins.json} ser encontrado dentro do jar do mod.</p>
 */
public final class TransformingClassLoader extends URLClassLoader {

    public TransformingClassLoader(List<URL> classpath, ClassLoader parent) {
        super(classpath.toArray(new URL[0]), parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] original = readBytes(name);
        if (original == null) {
            return super.findClass(name);
        }

        byte[] transformed = transform(name, original);
        if (transformed == null) {
            return super.findClass(name);
        }
        return defineClass(name, transformed, 0, transformed.length);
    }

    /**
     * Passa os bytes pelo transformer do Mixin.
     *
     * @return os bytes transformados, ou null se o Mixin nao tiver nada a
     *         fazer (ou nao estiver inicializado)
     */
    private byte[] transform(String name, byte[] input) {
        if (!MixinBootstrapper.isInitialized()) {
            return null;
        }
        Object active = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        if (!(active instanceof IMixinTransformer)) {
            // Mixin ainda nao criou o transformer: cai no classpath normal
            return null;
        }
        try {
            return ((IMixinTransformer) active)
                    .transformClass(MixinEnvironment.getCurrentEnvironment(), name, input);
        } catch (Throwable t) {
            // Uma classe que o Mixin nao conseguiu transformar nao pode derrubar
            // o jogo: devolve null e o ClassLoader usa os bytes originais.
            com.solarclient.launcher.util.LauncherLog.warn(
                    "Mixin nao transformou " + name + ": " + t);
            return null;
        }
    }

    private byte[] readBytes(String name) {
        String path = name.replace('.', '/') + ".class";
        try (InputStream in = getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
