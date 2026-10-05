#!/bin/sh
# ---------------------------------------------------------------------------
# Verificacao local do projeto SEM o jar do Minecraft.
#
#   1. gera os stubs da API 1.8.9 (so metodos usados pelo client)
#   2. baixa o Gson
#   3. compila :client, :launcher e :api com javac --release 8
#   4. confere o registro solar.mixins.json
#   5. confere o gradle.properties (chaves e comentarios colados)
#
# Isso valida tipagem, assinaturas e uso de API do NOSSO codigo. Nao substitui
# a compilacao real (docs/BUILD.md), mas pega a maioria dos erros de digitar.
#
# Uso: sh tools/verify/verify.sh
# ---------------------------------------------------------------------------
set -e
cd "$(dirname "$0")/../.."

echo "== 1/5 gerando stubs =="
node tools/verify/gen-stubs.js

echo "== 2/5 baixando o gson =="
mkdir -p tools/verify/lib
if [ ! -f tools/verify/lib/gson-2.8.9.jar ]; then
  curl -sSL -o tools/verify/lib/gson-2.8.9.jar \
    https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar
fi

GSON=tools/verify/lib/gson-2.8.9.jar

compile() {
  module=$1
  outdir=$2
  list=$3
  cp=$4
  rm -rf "$outdir"
  mkdir -p "$outdir"
  find tools/verify/stubs -name '*.java' > "$list"
  find "$module" -name '*.java' >> "$list"
  javac --release 8 -nowarn -proc:none -encoding UTF-8 -cp "$cp" -d "$outdir" @"$list"
  echo "OK: $module"
}

echo "== 3/5 compilando os modulos =="
compile client/src/main/java tools/verify/out-client tools/verify/sources-client.txt "$GSON"
compile launcher/src/main/java tools/verify/out-launcher tools/verify/sources-launcher.txt "$GSON"
compile api/src/main/java tools/verify/out-api tools/verify/sources-api.txt "$GSON"

echo "== 4/5 conferindo o registro de mixins =="
node tools/verify/check-mixins-json.js

echo "== 5/5 conferindo o gradle.properties =="
node tools/verify/check-gradle-properties.js

echo
echo "Tudo compila. Proximo passo: compilar de verdade (docs/BUILD.md)."
