# Como compilar de verdade

Este guia cobre o que a verificação com stubs **não** cobre: compilar contra o
jar real do Minecraft 1.8.9, gerar o jar do mod e rodar o launcher.

---

## 1. Java 8 (obrigatório)

O Minecraft 1.8.9 e o OptiFine rodam em Java 8. O código do projeto é
`--release 8`, então compilar com um JDK novo funciona — **rodar** o jogo
precisa do Java 8.

```bash
java -version        # deve ser 1.8.0_x
```

Se tiver mais de um Java instalado, aponte o launcher para o 8 com
`JAVA_HOME` (ou o `java.home` do processo).

---

## 2. O jar do Minecraft 1.8.9 decompilado e "mapped"

O build usa `client/libs/minecraft-1.8.9-mapped.jar` como `compileOnly`.
Esse arquivo **não está no repositório** (o jar do Minecraft não pode ser
redistribuído) e precisa ser gerado por você.

Ele tem que ser:

* a classe `net.minecraft.client.main.Main` do **1.8.9**;
* com **nomes MCP** (`EntityPlayerSP`, `RenderPlayer`, `ModelPlayer`,
  `ScaledResolution`, ...), porque é assim que o código está escrito.

### Opção A — a mais simples (recomendada para começar)

Baixe o jar oficial do 1.8.9, decompile e aplique os mappings:

```bash
# 1. jar oficial
curl -o client.jar https://launcher.mojang.com/v1/objects/f0c3f0a2b8a0b0e3e0d6b9d1f6f5f0a/client.jar
#    (a URL exata sai de https://launchermeta.mojang.com/mc/game/version_manifest.json)

# 2. decompile
java -jar cfr.jar client.jar --outputdir decomp

# 3. mappings MCP 1.8.9 (stable_22 ou similar)
curl -o mcp.zip https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable/22-1.8.9/mcp_stable-22-1.8.9.zip
unzip mcp.zip -d mcp

# 4. aplicar o deobfuscate (field.csv + methods.csv + params.csv)
#    -> qualquer ferramenta de remape serve (SpecialSource, Recaf, ...)
```

### Opção B — usar o ForgeGradle de uma pasta temporária

Se você já tem o ForgeGradle configurado em outro projeto, o truque é:

```bash
# num projeto vazio com ForgeGradle 2.1
gradle setupDecompWorkspace --deobf
cp build/minecraftRepo/minecraft/de/oceanlabs/mcp/mcp_stable/22-1.8.9/mcp_server.jar \
   /caminho/do/projeto/client/libs/minecraft-1.8.9-mapped.jar
```

### Opção C — um jar "mapeado" da comunidade

Vários projetos publicam o 1.8.9 já mapeado. **Confira a licença** do jar
antes de usar: ele não pode ser commitado neste repositório
(o `.gitignore` já ignora `client/libs/*.jar`).

### Como saber se deu certo

```bash
unzip -l client/libs/minecraft-1.8.9-mapped.jar | grep -c "net/minecraft/client/gui/ScaledResolution.class"
# tem que ser 1
```

---

## 3. Compilar os módulos

```bash
gradle :client:jar      # -> client/build/libs/solar-client-0.1.0.jar
gradle :launcher:jar    # -> launcher/build/libs/solar-client-launcher-0.1.0.jar
gradle :api:run         # sobe a API em http://localhost:8787
```

Se o `gradle` da máquina for um Gradle novo e reclamar de sintaxe, use o
Gradle 6/7 (`distributionUrl` em `gradle/wrapper/gradle-wrapper.properties`).
Não há wrapper commitado de propósito — o `gradle-wrapper.jar` é binário.

### Erros comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `package net.minecraft does not exist` | jar mapeado faltando/errado | refaça a seção 2 |
| `cannot find symbol: method X` | seu mapping usa outro nome | ajuste a chamada (ou o `@Inject(method=...)`) |
| `bad class file` | JDK com `--release` incompatível | use JDK 8 ou 11+ com `options.release` |
| Mixin: `InvalidInjectionException` em runtime | nome do método alvo diferente | veja a tabela em `docs/ARQUITETURA.md` |

---

## 4. Verificação sem o jar (CI)

```bash
sh tools/verify/verify.sh
```

Faz 5 coisas:

1. gera os stubs da API 1.8.9 (`gen-stubs.js`);
2. baixa o Gson;
3. compila `client`, `launcher` e `api` com `javac --release 8`;
4. confere que `solar.mixins.json` bate com as classes declaradas
   (`check-mixins-json.js`);
5. confere o `gradle.properties` (`check-gradle-properties.js`): nenhuma chave
   com comentário colado no valor e toda propriedade usada pelos `.gradle`
   realmente definida — o Gradle resolveria a dependência errada sem isso.

Serve como portão de qualidade no CI: rápido, offline (depois do primeiro
download do Gson) e sem depender do jar do Minecraft.

### Compilando `:api` e `:launcher` de verdade (sem o jar do MC)

Esses dois módulos **não** dependem do Minecraft, então dá para compilar e rodar
agora com as libs reais:

```bash
mkdir -p tools/verify/lib && cd tools/verify/lib
curl -sSO https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar
curl -sSO https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar
curl -sSO https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar
curl -sSO https://repo1.maven.org/maven2/org/ow2/asm/asm/9.2/asm-9.2.jar
curl -sSO https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.2/asm-tree-9.2.jar
curl -sSO https://repo1.maven.org/maven2/org/ow2/asm/asm-util/9.2/asm-util-9.2.jar
curl -sSO https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.2/asm-commons-9.2.jar
curl -sSO https://repo1.maven.org/maven2/org/ow2/asm/asm-analysis/9.2/asm-analysis-9.2.jar
curl -sSO https://repo1.maven.org/maven2/com/google/guava/guava/31.1-jre/guava-31.1-jre.jar
# o Mixin 0.8.5 esta no repo do SpongePowered (nao no Maven Central):
curl -sSO https://repo.spongepowered.org/repository/maven-public/org/spongepowered/mixin/0.8.5/mixin-0.8.5.jar
cd ../..

CP=$(ls tools/verify/lib/*.jar | tr '\n' ':')
mkdir -p build/classes/api build/classes/launcher
javac --release 8 -proc:none -cp "$CP" -d build/classes/api $(find api/src/main/java -name '*.java')
javac --release 8 -proc:none -cp "$CP" -d build/classes/launcher $(find launcher/src/main/java -name '*.java')

# sobe a API e testa o contrato que o mod consome
java -cp "build/classes/api:$CP" com.solarclient.api.CosmeticsApiServer
curl localhost:8787/v1/health
curl localhost:8787/v1/cosmetics/catalog
curl -X POST -H 'Content-Type: application/json' \
  -d '{"uuid":"1a2b3c4d-0000-4000-8000-000000000001","slots":{"CAPE":"solar_cape"}}' \
  localhost:8787/v1/loadout
curl 'localhost:8787/v1/cosmetics/loadout?uuid=1a2b3c4d-0000-4000-8000-000000000001'
```

Isso **foi executado** e passou (ver o README, seção "Verificações").

### Empacotando os jars sem Gradle

O repositório não traz o `gradle-wrapper.jar` (é binário) e muita máquina não
tem Gradle instalado. Para `:launcher` e `:api` — que **não** dependem do jar do
Minecraft — dá para empacotar com `javac` + `jar`:

```bash
sh tools/verify/build-launcher-jar.sh
# -> build/dist/solar-client-launcher-0.1.0.jar
# -> build/dist/solar-client-api-0.1.0.jar
```

São "uber jars" (as libs dentro), então rodam com `java -jar` sem `-cp`. Três
cuidados que o script já aplica e que são fáceis de esquecer:

1. **`META-INF/*.SF|.DSA|.RSA` são removidos.** As libs são assinadas e, depois
   de reempacotar, o digest não bate mais — o JVM morre com
   `SecurityException: Invalid signature file digest for Manifest main attributes`.
2. **`src/main/resources` é copiado por último.** O Mixin traz o próprio
   `META-INF/services/...IMixinService`, apontando para LaunchWrapper e
   ModLauncher; se ele for copiado depois, ele sobrescreve o registro do projeto
   e o Mixin volta a procurar o Forge.
3. O `asm-util` precisa ser baixado (não vem por padrão na lista antiga).

O `:client` **não** sai por esse caminho: ele precisa do
`minecraft-1.8.9-mapped.jar` (seção 2).

### Sobre a versão do Mixin

O launcher usa a API do **Mixin 0.8.5** (`org.spongepowered.asm.launch.MixinBootstrap`
e `IMixinTransformer#transformClass`), que é a versão realmente compilada aqui.
Para voltar a uma versão antiga (0.6.x) seria preciso trocar o import do
bootstrap e a interface do transformer em
`launcher/src/main/java/com/solarclient/launcher/mixin/`.

O annotation processor do Mixin (que gera o `solar.refmap.json`) precisa do
Guava no classpath de compilação - ele já está declarado em `client/build.gradle`.

### Rodar o Mixin sem Forge: os três serviços que o launcher registra

Este é o ponto mais traiçoeiro do projeto, porque **nada disso dá erro de
compilação** — só quebra na hora de subir o jogo.

O Mixin não funciona sozinho. Quando `MixinBootstrap.init()` roda, ele procura
um `IMixinService` por `ServiceLoader`, e o jar oficial do Mixin 0.8.5 só traz
registro para dois hospedeiros:

| Serviço | Quem implementa | Funciona aqui? |
|---|---|---|
| `IMixinService` | `MixinServiceLaunchWrapperBootstrap` (Forge) | **não** |
| `IMixinService` | `MixinServiceModLauncherBootstrap` (Fabric) | **não** |
| `IGlobalPropertyService` | LaunchWrapper / ModLauncher | **não** |

Num setup sem Forge e sem Fabric, `init()` estoura:

```
org.spongepowered.asm.service.ServiceNotAvailableError:
    No mixin host service is available.
```

Por isso o launcher carrega os registros dele em
`launcher/src/main/resources/META-INF/services/`:

* `org.spongepowered.asm.service.IMixinService` → `SolarMixinService`
  (implementa os providers mínimo; a transformação de verdade continua no
  `TransformingClassLoader`);
* `org.spongepowered.asm.service.IGlobalPropertyService` → `SolarPropertyService`
  (as propriedades globais do Mixin).

E ainda falta uma terceira peça: **quem cria o `MixinTransformer`.** No Forge é
o `Proxy` do próprio Mixin — mas essa classe implementa
`net.minecraft.launchwrapper.IClassTransformer`, então nem carrega sem o Forge:

```
java.lang.NoClassDefFoundError: net/minecraft/launchwrapper/IClassTransformer
```

O launcher contorna chamando a `MixinTransformer$Factory` (package-private) por
reflexão, em `MixinBootstrapper.ensureTransformer()`. Dois detalhes que só
aparecem em execução:

* **tem que ser depois do `addConfiguration`** — o construtor do
  `MixinTransformer` lê a lista de alvos das configs na hora em que é criado. Se
  ele nascer antes, guarda uma lista vazia para sempre e nenhuma classe é
  transformada, **sem erro nenhum**;
* **`getActiveTransformer()` devolve `null` até isso acontecer** — o
  `TransformingClassLoader` checa isso e cai silenciosamente nos bytes
  originais quando é `null`.

As libs de ASM (`asm`, `asm-tree`, `asm-util`, `asm-commons`, `asm-analysis`)
precisam estar no classpath **de runtime**: o verificador do Mixin usa
`org.objectweb.asm.util.CheckClassAdapter`, e sem o `asm-util` o launcher
quebra com `NoClassDefFoundError`.

Para conferir que o ambiente subiu (não depende do jar do Minecraft):

```bash
sh tools/verify/build-launcher-jar.sh
mkdir -p build/smoke
javac -encoding UTF-8 -cp build/dist/solar-client-launcher-0.1.0.jar \
      -d build/smoke tools/verify/Smoke.java
java -cp build/dist/solar-client-launcher-0.1.0.jar:build/smoke Smoke
# esperado: SMOKE_OK
```

---

## 5. Rodar o launcher

```bash
# modo offline
java -jar launcher/build/libs/solar-client-launcher-0.1.0.jar --user SeuNome

# login Microsoft (precisa de microsoftClientId em gradle.properties)
java -jar launcher/build/libs/solar-client-launcher-0.1.0.jar --user SeuNome --online

# sem OptiFine
java -jar launcher/build/libs/solar-client-launcher-0.1.0.jar --user SeuNome --no-optifine
```

O launcher:

1. baixa o 1.8.9, as libraries e os assets em `cache/` (só o que falta);
2. roda o instalador do OptiFine (o jar dele precisa estar em
   `cache/OptiFine_1.8.9_HD_U_M5.jar`; baixe em <https://optifine.net/download>);
3. registra o Mixin e carrega as classes do jogo por um
   `TransformingClassLoader` (as classes são reescritas **em memória**);
4. chama `net.minecraft.client.main.Main` com a sessão (Microsoft ou offline).

### Casca em Electron (opcional)

```bash
cd electron-shell
npm install
npm start
```

Ela não reimplementa nada: cria a janela, mostra o log e roda o launcher Java.

---

## 6. Onde ficam as coisas em tempo de execução

| Caminho | O que é |
|---------|---------|
| `solar-client/config.json` | mods ligados, posições, escalas, tema, sessão |
| `solar-client/cosmetics.json` | cosméticos equipados por UUID |
| `cache/` | jar do jogo, libraries, assets, OptiFine gerado |
| `assets/indexes/1.8.json` | índice de assets (criado pelo launcher) |
| `data/cosmetics.db` | banco da API (se você rodar local) |
