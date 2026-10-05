#!/bin/sh
# Empacota os jars do projeto SEM Gradle.
#
# Por que existe: o repositorio nao traz o gradle-wrapper e a maquina de build
# nao tem Gradle instalado. Este script faz o mesmo trabalho com javac + jar:
#
#   - compila com --release 8 (mesmo alvo do build.gradle);
#   - gera um "uber jar" com as libs dentro, para rodar com `java -jar`;
#   - remove META-INF/*.SF|.DSA|.RSA (senao o JVM acusa
#     "Invalid signature file digest for Manifest main attributes");
#   - copia src/main/resources DEPOIS das libs, para que os registros
#     META-INF/services do projeto (IMixinService, IGlobalPropertyService)
#     nao sejam sobrescritos pelos do Mixin, que so apontam para
#     LaunchWrapper (Forge) e ModLauncher (Fabric).
#
# Uso: sh tools/verify/build-launcher-jar.sh
# Saida: build/dist/solar-client-launcher-<versao>.jar e solar-client-api-<versao>.jar
set -e

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"

L=tools/verify/lib
OUT=build/dist
VERSION=$(sed -n 's/^modVersion=//p' gradle.properties | head -1)

mkdir -p "$OUT"

# ------------------------------------------------------------------ launcher
LAUNCHER_CP="$L/gson-2.8.9.jar:$L/mixin-0.8.5.jar:$L/asm-9.2.jar:$L/asm-tree-9.2.jar:$L/asm-util-9.2.jar:$L/asm-commons-9.2.jar:$L/asm-analysis-9.2.jar:$L/guava-31.1-jre.jar"
LAUNCHER_LIBS="gson-2.8.9 mixin-0.8.5 asm-9.2 asm-tree-9.2 asm-util-9.2 asm-commons-9.2 asm-analysis-9.2 guava-31.1-jre"

rm -rf build/build-launcher
mkdir -p build/build-launcher
find launcher/src/main/java -name '*.java' > build/srcs-launcher.txt
javac --release 8 -nowarn -encoding UTF-8 -cp "$LAUNCHER_CP" -d build/build-launcher @build/srcs-launcher.txt

for j in $LAUNCHER_LIBS; do unzip -oq "$L/$j.jar" -d build/build-launcher; done
find build/build-launcher -name 'module-info.class' -delete
find build/build-launcher -path '*/META-INF/*' \( -name '*.SF' -o -name '*.DSA' -o -name '*.RSA' \) -delete
cp -r launcher/src/main/resources/. build/build-launcher/
jar cfe "$OUT/solar-client-launcher-$VERSION.jar" com.solarclient.launcher.Launcher -C build/build-launcher .
echo "gerado: $OUT/solar-client-launcher-$VERSION.jar"

# ----------------------------------------------------------------------- api
API_CP="$L/gson-2.8.9.jar:$L/sqlite-jdbc-3.42.0.0.jar"
rm -rf build/build-api
mkdir -p build/build-api
find api/src/main/java -name '*.java' > build/srcs-api.txt
javac --release 8 -nowarn -encoding UTF-8 -cp "$API_CP" -d build/build-api @build/srcs-api.txt

for j in gson-2.8.9 sqlite-jdbc-3.42.0.0; do unzip -oq "$L/$j.jar" -d build/build-api; done
find build/build-api -name 'module-info.class' -delete
find build/build-api -path '*/META-INF/*' \( -name '*.SF' -o -name '*.DSA' -o -name '*.RSA' \) -delete
if [ -d api/src/main/resources ]; then cp -r api/src/main/resources/. build/build-api/; fi
jar cfe "$OUT/solar-client-api-$VERSION.jar" com.solarclient.api.CosmeticsApiServer -C build/build-api .
echo "gerado: $OUT/solar-client-api-$VERSION.jar"
