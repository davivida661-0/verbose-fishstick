import com.solarclient.launcher.mixin.MixinBootstrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Smoke test do bootstrap do Mixin — <b>sem o jar do Minecraft</b>.
 *
 * <p>Este e o teste que pega o problema mais chato do projeto: como o launcher
 * roda sem Forge e sem Fabric, o {@code MixinBootstrap.init()} precisa dos
 * servicos que o launcher registra em {@code META-INF/services/}. Sem eles o
 * init estoura {@code ServiceNotAvailableError} e o jogo nem abre.</p>
 *
 * <p>Cobre a cadeia inteira:</p>
 * <ol>
 *     <li>{@code init()} sobe com os servicos do proprio launcher;</li>
 *     <li>{@code apply()} acha um {@code solar.mixins.json} minimo;</li>
 *     <li>o transformer do Mixin e criado e registrado — sem isso o
 *         {@code TransformingClassLoader} cai silenciosamente nos bytes
 *         originais e o mod nao faz nada.</li>
 * </ol>
 *
 * <p>Nao valida a aplicacao do mixin em si: para isso seria preciso o jar do
 * Minecraft (ver {@code docs/BUILD.md}).</p>
 *
 * <p>Uso:</p>
 * <pre>
 *   sh tools/verify/build-launcher-jar.sh
 *   javac -encoding UTF-8 -cp build/dist/solar-client-launcher-0.1.0.jar \
 *         -d build/smoke tools/verify/Smoke.java
 *   java -cp build/dist/solar-client-launcher-0.1.0.jar:build/smoke Smoke
 *   # esperado: SMOKE_OK
 * </pre>
 */
public final class Smoke {

    public static void main(String[] args) throws Exception {
        MixinBootstrapper.init();
        System.out.println("isInitialized=" + MixinBootstrapper.isInitialized());
        System.out.println("env=" + MixinBootstrapper.getCurrentEnvironment().getClass().getName());

        // Carrega de verdade uma classe do Mixin e uma do ASM: e o que a
        // assinatura de IMixinTransformer#transformClass referencia, entao se
        // faltar asm-tree no classpath quebra aqui.
        System.out.println("IMixinTransformer=" + Class.forName(
                "org.spongepowered.asm.mixin.transformer.IMixinTransformer").getName());
        System.out.println("ClassNode=" + Class.forName(
                "org.objectweb.asm.tree.ClassNode").getName());

        // apply() precisa de um jar com o config; monta um minimo em memoria.
        File modJar = File.createTempFile("solar-smoke", ".jar");
        modJar.deleteOnExit();
        writeConfig(modJar);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{modJar.toURI().toURL()}, Smoke.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        MixinBootstrapper.apply(modJar);
        loader.close();

        System.out.println("transformer=" + MixinBootstrapper.getTransformer());
        if (MixinBootstrapper.getTransformer() == null) {
            throw new IllegalStateException("transformer do Mixin nao foi criado");
        }

        System.out.println("SMOKE_OK");
    }

    /** Jar de mod com um {@code solar.mixins.json} vazio (so para o apply). */
    private static void writeConfig(File jar) throws Exception {
        String json = "{\"required\":false,\"minVersion\":\"0.8.0\",\"package\":\"\","
                + "\"compatibilityLevel\":\"JAVA_8\",\"client\":[],"
                + "\"server\":[],\"mixins\":[]}\n";
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry("solar.mixins.json"));
            out.write(json.getBytes("UTF-8"));
            out.closeEntry();
        }
    }
}
