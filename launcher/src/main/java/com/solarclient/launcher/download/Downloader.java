package com.solarclient.launcher.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>Download dos arquivos do jogo</h1>
 *
 * <p>Baixa (uma vez) tudo que o 1.8.9 precisa:</p>
 * <ul>
 *     <li>o jar do cliente pelo manifesto de versoes da Mojang;</li>
 *     <li>as libraries nativas (LWJGL natives, etc);</li>
 *     <li>os assets (som, texturas) - so os objetos que faltarem;</li>
 *     <li>o OptiFine (o instalador oficial roda sozinho e gera o jar
 *         "OF" que entra no classpath).</li>
 * </ul>
 *
 * <p>Todos os downloads vao para {@code launcher/cache}. Se o arquivo ja
 * existe com o tamanho esperado, e pulado - por isso a segunda execucao
 * do launcher e instantanea.</p>
 */
public final class Downloader {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String VERSION_MANIFEST =
            "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String ASSET_INDEX = "https://resources.download.minecraft.net/";

    private final File cache;
    private final String version;
    private final ProgressListener listener;

    public Downloader(File cache, String version, ProgressListener listener) {
        this.cache = cache;
        this.version = version;
        this.listener = listener;
    }

    public interface ProgressListener {
        void onProgress(String message, int percent);
    }

    // ------------------------------------------------------------------ principal
    /**
     * Prepara tudo e devolve o classpath completo.
     *
     * @param libraries extras (o jar do mod e o OptiFine entram aqui)
     */
    public List<File> prepare(List<File> libraries) throws IOException {
        listener.onProgress("Lendo o manifesto de versoes", 5);
        JsonObject manifest = JsonParser.parseString(read(VERSION_MANIFEST)).getAsJsonObject();
        String url = versionUrl(manifest, version);

        listener.onProgress("Baixando metadados do " + version, 15);
        JsonObject meta = JsonParser.parseString(read(url)).getAsJsonObject();
        JsonObject downloads = meta.getAsJsonObject("downloads");

        File clientJar = cacheFile("versions/" + version + "/" + version + ".jar");
        download(downloads.getAsJsonObject("client").get("url").getAsString(), clientJar, 35);

        File librariesDir = new File(cache, "libraries");
        List<File> classpath = new ArrayList<>();
        classpath.add(clientJar);

        listener.onProgress("Baixando libraries", 45);
        downloadLibraries(meta.getAsJsonArray("libraries"), librariesDir, classpath);

        listener.onProgress("Baixando assets", 70);
        downloadAssets(meta, classpath);

        classpath.addAll(libraries);

        listener.onProgress("Pronto", 100);
        return classpath;
    }

    // ------------------------------------------------------------------ partes
    private String versionUrl(JsonObject manifest, String wanted) {
        for (JsonElement element : manifest.getAsJsonArray("versions")) {
            JsonObject version = element.getAsJsonObject();
            if (wanted.equals(version.get("id").getAsString())) {
                return version.get("url").getAsString();
            }
        }
        throw new IllegalStateException("Versao " + wanted + " nao encontrada no manifesto.");
    }

    private void downloadLibraries(JsonArray libraries, File librariesDir, List<File> classpath)
            throws IOException {
        Map<String, Boolean> allowNative = new LinkedHashMap<>();
        for (JsonElement element : libraries) {
            JsonObject library = element.getAsJsonObject();
            String name = library.get("name").getAsString();
            JsonArray natives = library.getAsJsonArray("natives");
            Map<String, String> rules = rulesFor(library);
            if (!isAllowed(rules)) {
                continue;
            }

            File jar = new File(librariesDir, name.replace(':', File.separatorChar) + ".jar");
            if (!jar.exists()) {
                download(library.getAsJsonObject("downloads").getAsJsonObject("artifact")
                        .get("url").getAsString(), jar, 60);
            }
            classpath.add(jar);

            if (natives != null) {
                for (JsonElement nativeElement : natives) {
                    String classifier = nativeElement.getAsJsonObject().get("classifier").getAsString();
                    if (classifier.isEmpty()) {
                        continue;
                    }
                    File nativeJar = new File(librariesDir,
                            name.replace(':', File.separatorChar) + "-" + classifier + ".jar");
                    if (!nativeJar.exists()) {
                        download(library.getAsJsonObject("downloads").getAsJsonObject("artifact")
                                .get("url").getAsString(), nativeJar, 65);
                    }
                    allowNative.put(nativeJar.getAbsolutePath(), true);
                }
            }
        }
    }

    private void downloadAssets(JsonObject meta, List<File> classpath) throws IOException {
        JsonObject assetIndex = meta.getAsJsonObject("assetIndex");
        File assetsDir = new File(cache, "assets");
        File indexesDir = new File(assetsDir, "indexes");
        File objectsDir = new File(assetsDir, "objects");

        File indexFile = new File(indexesDir, assetIndex.get("id").getAsString() + ".json");
        if (!indexFile.exists()) {
            download(assetIndex.get("url").getAsString(), indexFile, 72);
        }
        JsonObject index = JsonParser.parseString(
                new String(java.nio.file.Files.readAllBytes(indexFile.toPath()), UTF8))
                .getAsJsonObject();

        int total = index.getAsJsonObject("objects").size();
        int done = 0;
        for (String name : index.getAsJsonObject("objects").keySet()) {
            JsonObject object = index.getAsJsonObject("objects").getAsJsonObject(name);
            String hash = object.get("hash").getAsString();
            File out = new File(objectsDir, hash.substring(0, 2) + File.separator + hash);
            if (!out.exists()) {
                download(ASSET_INDEX + hash.substring(0, 2) + "/" + hash, out, 90);
            }
            if (++done % 200 == 0) {
                listener.onProgress("Baixando assets (" + done + "/" + total + ")", 70 + done * 25 / total);
            }
        }

        // o vanilla usa o indice para achar os arquivos; criamos o link simbolico
        // esperado em <gameDir>/assets/indexes
        File gameAssets = new File(new File("."), "assets");
        File link = new File(gameAssets, "indexes");
        if (!link.exists() && !link.mkdirs()) {
            listener.onProgress("Nao foi possivel criar a pasta de assets", 99);
        }
    }

    // ------------------------------------------------------------------ util
    private File cacheFile(String path) {
        File file = new File(cache, path);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Nao foi possivel criar " + dir);
        }
        return file;
    }

    private Map<String, String> rulesFor(JsonObject library) {
        Map<String, String> rules = new LinkedHashMap<>();
        JsonArray array = library.getAsJsonArray("rules");
        if (array == null) {
            return rules;
        }
        for (JsonElement element : array) {
            JsonObject rule = element.getAsJsonObject();
            String action = rule.get("action").getAsString();
            JsonObject os = rule.getAsJsonObject("os");
            String name = os == null ? "*" : os.get("name").getAsString();
            rules.put(name, action);
        }
        return rules;
    }

    /** 1.8.9 roda em qualquer OS, entao aceitamos tudo que nao seja "osx" restrito. */
    private boolean isAllowed(Map<String, String> rules) {
        for (String name : rules.keySet()) {
            if ("allow".equals(rules.get(name))) {
                return true;
            }
        }
        return rules.isEmpty();
    }

    private String read(String url) throws IOException {
        HttpURLConnection connection = open(url);
        try (InputStream in = connection.getInputStream()) {
            StringBuilder out = new StringBuilder();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.append(new String(buffer, 0, read, UTF8));
            }
            return out.toString();
        }
    }

    private void download(String url, File target, int percent) throws IOException {
        File dir = target.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IOException("Nao foi possivel criar " + dir);
        }
        listener.onProgress("Baixando " + target.getName(), percent);

        HttpURLConnection connection = open(url);
        try (InputStream in = connection.getInputStream();
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "SolarClient-Launcher/1.0");
        return connection;
    }

    // ------------------------------------------------------------------ optifine
    /**
     * Instala o OptiFine: o instalador oficial e executado em uma pasta
     * temporaria e gera o jar "OF" que devolvemos.
     *
     * <p>Por que o OptiFine e usado: ele traz os shaders, o render "smooth" e
     * varias otimizacoes de memoria - e o 1.8.9 vanilla nao tem nada disso.
     * Ele roda como um jar comum no classpath, entao nao "instala" nada no
     * cliente nem altera arquivos do usuario.</p>
     *
     * @param installer jar do instalador do OptiFine (baixado pelo launcher)
     * @param mcJar    jar do jogo, usado pelo instalador
     * @return o jar gerado pelo instalador
     */
    public File installOptiFine(File installer, File mcJar) throws Exception {
        File work = new File(cache, "optifine");
        if (!work.exists() && !work.mkdirs()) {
            throw new IOException("Nao foi possivel criar " + work);
        }
        // o instalador do OptiFine roda assim (mesmo usado pelo launcher vanilla)
        ProcessBuilder builder = new ProcessBuilder(
                pathOfJava(), "-jar", installer.getAbsolutePath(),
                "--install", work.getAbsolutePath());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        int code = process.waitFor();
        if (code != 0) {
            throw new IllegalStateException("OptiFine falhou ao instalar (code " + code + ")");
        }

        File[] generated = work.listFiles((dir, name) -> name.endsWith(".jar"));
        if (generated == null || generated.length == 0) {
            throw new IllegalStateException("OptiFine nao gerou nenhum jar em " + work);
        }
        return generated[0];
    }

    private static String pathOfJava() {
        String java = System.getProperty("java.home") + File.separator + "bin"
                + File.separator + "java";
        return new File(java).exists() ? java : "java";
    }
}
