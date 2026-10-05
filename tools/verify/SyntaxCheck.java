import com.sun.source.util.JavacTask;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Verificador de SINTAXE dos fontes Java do projeto.
 *
 * <p>Usa a API do compilador do proprio JDK (JavacTask#parse) para checar
 * apenas o parse de todos os arquivos - nao resolve simbolos, entao nao
 * precisa do jar do Minecraft nem das libraries para rodar.
 *
 * <p>Uso: {@code java tools/verify/SyntaxCheck.java client/src/main/java}</p>
 *
 * <p>Ele pega erro de digitacao, chave faltando, parenteses e ponto e virgula.
 * Erro de nome de metodo/classe so aparece na compilacao de verdade
 * (veja docs/BUILD.md).</p>
 */
public class SyntaxCheck {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("uso: java SyntaxCheck.java <pasta>");
            System.exit(2);
        }

        List<File> files = new ArrayList<>();
        for (String arg : args) {
            collect(new File(arg), files);
        }
        if (files.isEmpty()) {
            System.out.println("nenhum arquivo .java encontrado");
            return;
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> units =
                    fileManager.getJavaFileObjectsFromFiles(files);

            JavacTask task = (JavacTask) compiler.getTask(
                    null, fileManager, diagnostics,
                    Arrays.asList("-proc:none", "-Xlint:none", "-encoding", "UTF-8"),
                    null, units);

            // so o parse: rapido e sem depender de classpath
            task.parse();
        }

        int errors = 0;
        for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            errors++;
            System.out.println("ERRO " + diagnostic.getSource()
                    + ":" + diagnostic.getLineNumber() + " " + diagnostic.getMessage(null));
        }

        System.out.println(errors == 0
                ? "OK: " + files.size() + " arquivos, 0 erro de sintaxe"
                : "FALHA: " + errors + " erro(s) de sintaxe em " + files.size() + " arquivos");
        System.exit(errors == 0 ? 0 : 1);
    }

    private static void collect(File file, List<File> out) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children);
                for (File child : children) {
                    collect(child, out);
                }
            }
        } else if (file.getName().endsWith(".java")) {
            out.add(file);
        }
    }
}
