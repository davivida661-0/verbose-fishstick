# Solar Client

Client de Minecraft **1.8.9** focado em PvP e BedWars, no estilo Lunar/Badlion:
sem cheat (nada de KillAura, Velocity, Reach ou Fly), **sem depender de Forge**
(injeção por **Mixin**), com OptiFine integrado, launcher próprio e sistema de
cosméticos sincronizado por API.

> O nome é um exemplo. Para renomear tudo para o seu:  
> `grep -rl "Solar" --include=*.java --include=*.gradle --include=*.json --include=*.md . | xargs sed -i 's/SolarClient/SeuClient/g; s/Solar /Seu /g; s/solarclient/seuclient/g; s/solar/seu/g'`

---

## 1. O que já está pronto aqui

| Módulo | Arquivos | Estado |
|--------|----------|--------|
| Núcleo (classe principal, ModManager, eventos, config JSON) | ~15 | completo, comentado em PT-BR |
| Mods PvP + HUD (13) | 13 | completos |
| Mods de Render (10) | 10 | completos |
| Mods BedWars (10) | 10 | completos (Auto* desligados por padrão) |
| Performance (1 com 4 perfis) | 1 | completo |
| Sistema de cosméticos + capa animada | 8 | completo (1 cosmético de exemplo) |
| ClickGUI, HUD Editor, GUI de cosméticos, atalhos | 5 | completo |
| Mixins (10 pontos de injeção, inclusive menu principal) | 1 | completo |
| API REST de cosméticos (SQLite) | 3 | completo |
| Launcher (login Microsoft + download + OptiFine + Mixin) | 6 | completo |
| Casca Electron do launcher | 4 | completo |
| Documentação (build, API, arquitetura) | 3 | completa |

**Mods de exemplo pedidos, escritos passo a passo em português:**

| Exemplo | Arquivo |
|---------|---------|
| 1. Keystrokes (WASD + mouse + espaço) | [`client/src/main/java/com/solarclient/mods/pvp/Keystrokes.java`](client/src/main/java/com/solarclient/mods/pvp/Keystrokes.java) |
| 2. CPS Counter | [`client/src/main/java/com/solarclient/mods/pvp/CpsCounter.java`](client/src/main/java/com/solarclient/mods/pvp/CpsCounter.java) |
| 3. 1.7 Animations | [`client/src/main/java/com/solarclient/mods/render/OneSevenAnimations.java`](client/src/main/java/com/solarclient/mods/render/OneSevenAnimations.java) |
| 4. Animated Cape (cosmético) | [`client/src/main/java/com/solarclient/cosmetics/AnimatedCape.java`](client/src/main/java/com/solarclient/cosmetics/AnimatedCape.java) |

---

## 2. Estrutura de pastas

```
solar-client/
├── settings.gradle / build.gradle / gradle.properties
├── README.md
│
├── client/                              # O MOD (Java 8 + Mixin + 1.8.9)
│   ├── build.gradle
│   ├── libs/                            # <- minecraft-1.8.9-mapped.jar (veja docs/BUILD.md)
│   └── src/main/
│       ├── java/com/solarclient/
│       │   ├── SolarClient.java         # classe principal (init, tick, shutdown)
│       │   ├── config/ConfigManager     # config JSON (posições, valores, tema)
│       │   ├── event/                   # EventBus + Events (Tick, Render2D, Render3D, Chat...)
│       │   ├── mod/                     # Mod, HudMod, ModManager, ModRegistry, ModSettings
│       │   │   └── mods/
│       │   │       ├── pvp/             # Keystrokes, Cps, Fps, Ping, Armor, Potion...
│       │   │       ├── render/          # 1.7 Animations, NameTags, FullBright, ClearGlass...
│       │   │       └── performance/     # FPS Boost, Memory Fix, Entity Culling, Lag Patch
│       │   ├── bedwars/                 # Overlay, Session, AutoGG, InvisibleWarning...
│       │   ├── cosmetics/               # CosmeticManager, Rarity, CosmeticType, AnimatedCape
│       │   ├── network/                 # CosmeticNetwork (sync entre jogadores)
│       │   ├── gui/                     # ClickGUI, HudEditorGui, CosmeticsGui, GuiWidgets
│       │   ├── mixin/SolarMixin         # os 9 pontos de injeção
│       │   └── ui/                      # RenderUtils, SolarFont (TTF), Blur, Theme, Colors
│       └── resources/
│           ├── solar.mixins.json        # registro dos mixins
│           └── assets/solarclient/      # fontes .ttf e texturas dos cosméticos
│
├── launcher/                            # Launcher standalone (Java 8)
│   └── src/main/java/com/solarclient/launcher/
│       ├── Launcher.java                # main (offline / Microsoft device code)
│       ├── SolarGameLauncher.java       # monta classpath, sobe Mixin, chama o jogo
│       ├── auth/AuthManager             # Xbox Live -> XSTS -> Minecraft
│       ├── download/Downloader          # jogo, libraries, assets, OptiFine
│       └── mixin/                       # bootstrap do Mixin + classloader transformador
│
├── api/                                 # API REST de cosméticos (Java 8 puro)
│   ├── sql/schema.sql                   # tabelas + seed
│   └── src/main/java/com/solarclient/api/
│       ├── CosmeticsApiServer           # com.sun.net.httpserver, rotas
│       ├── Database                     # SQLite (ou Postgres, ver docs)
│       └── Json                         # resposta + X-Api-Key
│
├── electron-shell/                      # Casca visual do launcher (opcional)
│   ├── main.js / preload.js / renderer.js / index.html
│
├── docs/
│   ├── BUILD.md                         # como compilar de verdade
│   ├── ARQUITETURA.md                   # como tudo se encaixa
│   └── API-COSMETICS.md                 # contrato da API
│
└── tools/verify/                        # verificação sem o jar do Minecraft
    ├── gen-stubs.js                     # gera os stubs da API 1.8.9
    ├── verify.sh                        # compila tudo com javac --release 8
    └── SyntaxCheck.java                 # checa só a sintaxe
```

---

## 3. Como compilar

```bash
# 1) colocar o jar do Minecraft 1.8.9 decompilado/mapeado
#    em client/libs/minecraft-1.8.9-mapped.jar  (instruções em docs/BUILD.md)

# 2) compilar
gradle :client:jar

# 3) verificar sem o jar do Minecraft (útil no CI)
sh tools/verify/verify.sh
```

### O que já foi verificado aqui

| Verificação | Resultado |
|-------------|-----------|
| `SyntaxCheck` (parse dos 85 arquivos) | **0 erro** |
| `verify.sh` — `client` + `launcher` + `api` com `javac --release 8` | **exit 0** |
| `check-mixins-json.js` | **10/10 mixins** declarados e registrados |
| `check-gradle-properties.js` | **8 chaves** referenciadas, todas definidas (nenhum comentário colado em valor) |
| `:api` compilado com **gson + sqlite-jdbc reais** | **OK** |
| `:launcher` compilado com **gson + Mixin 0.8.5 + ASM reais** | **OK** (após corrigir imports escritos de memória) |
| API REST ligada e testada por `curl` | **health, catalog, loadout (GET/POST), owned, unlock e 404 respondem certo** |
| Imports não usados | **nenhum** |

O que **não** foi verificado: o módulo `:client` contra o jar real do Minecraft
(ele não pode ser redistribuído) e qualquer comportamento em runtime — ver
`docs/BUILD.md`.

<details>
<summary>Verificação rápida (o que ela cobre)</summary>

`tools/verify/verify.sh` gera stubs mínimos da API 1.8.9 (só os métodos que o
client usa) e compila `client`, `launcher` e `api` com `javac --release 8`.
Isso pega erro de tipo, assinatura, import e uso de API **do nosso código**.

Não substitui a compilação real: nomes de campo privados podem ter mudado no
seu mapping, e a API do Minecraft é validada contra os stubs, não contra o jar.
`tools/verify/SyntaxCheck.java` faz uma checagem ainda mais rápida (só parse).
</details>

---

## 4. Catálogo de mods

Todos abrem e fecham pelo **ClickGUI** (tecla `RS`) e têm posição/escala
arrastável no **HUD Editor** (tecla `HU`).

**PvP / HUD** — Keystrokes, CPS Counter, FPS, Ping, Armor Status, Potion Status,
Item Info, Pack Display, Tab List, Scoreboard, BossBar, Chat.

**Render** — 1.7 Animations (blockhit, old sneak, old rod), NameTags,
ToggleSprint, ToggleSneak, FullBright, Clear Glass, No Hurt Cam, Hit Color,
Coordinates, Direction, Time, Performance.

**BedWars** — BedWars Overlay (recursos, forja, equipes), Session (kills/beds/
wins), Fireball Jump, Low Health, Nick Hider, Invisible Warning, Auto GG,
Auto Play Again, Auto Tip.

**Performance** — um mod com quatro perfis independentes: FPS Boost, Memory Fix,
Entity Culling e Lag Patch.

### Teclas padrão

| Combo | Abre |
|-------|------|
| `R` + `S` | ClickGUI |
| `H` + `U` | HUD Editor |
| `C` + `O` | GUI de cosméticos |

São **combos** (não tecla solta) para não brigar com os atalhos dos mods nem
com os do jogo, e funcionam em **qualquer tela** — inclusive no menu principal,
onde o client desenha a logo `SOLAR CLIENT` e a dica do atalho. Os combos ficam
salvos em `solar-client/config.json`, na seção `gui`.

Cada mod tem a sua tecla (visível no card do ClickGUI e no menu de controles do
próprio Minecraft, porque o atalho é um `KeyBinding` vanilla).

### BedWars Overlay

Mostra ferro/ouro/diamante/esmeralda, tier da forja, as equipes e, para cada
uma, o **status da cama** (`CAMA OK` / `CAMA QUEBRADA`) e `ELIMINADA`. A sidebar
do servidor não informa o estado da cama, então ele vem do kill feed — os
padrões de frase estão centralizados em `BedStatusTracker` para você adaptar
ao servidor que você joga.

---

## 5. Sobre anticheat e regras de servidor

Tudo aqui é **visual, de HUD ou de QoL**: nenhum modulo envia pacote de
movimento, muda hitbox, aumenta alcance ou automatiza mira. Por isso o client
não deve disparar nenhuma regra de anticheat **pelo que ele faz** — mas isso
não é garantia de nada:

* **Servidores podem banir pelo simples uso de client.** Leia as regras de
  Hypixel, Grim, Vulcan etc. antes de jogar em ranked.
* **Auto GG / Auto Play Again / Auto Tip** são automação de chat: muitos
  servidores tratam isso como macro. Vêm **desligados** por padrão e é por sua
  conta e risco.
* **Invisible Warning** só mostra um aviso (som + texto) quando alguém fica
  invisível a menos de 4 blocos, sem ESP, sem posição, sem distância — e vem
  **desligado**. Mesmo assim, automatizar reação a inimigo invisível pode ser
  interpretado como vantagem.

O resto (Keystrokes, CPS, FullBright, NameTags, HUD) é o mesmo tipo de recurso
que o Lunar e o Badlion oferecem.

---

## 6. Cosméticos

* Tipos: `CAPE`, `CLOAK`, `DRAGON_WINGS`, `ANGEL_WINGS`, `BANDANA`, `HAT`, `GLASSES`.
* Raridades: `COMMON`, `RARE`, `EPIC`, `LEGENDARY` (cor + brilho pulsante).
* O equipamento é salvo por **UUID** em `solar-client/cosmetics.json` e
  sincronizado com a API REST.
* Para **outros jogadores do Solar Client** verem a sua capa, o cliente manda um
  payload customizado no canal `solar|cosmetics` (o servidor repassa; quem não
  tem o client vê o jogador normal). Nenhum dado de jogo trafega nele.
* A capa animada é desenhada como uma **malha 6×10 deformada por senoide** — o
  topo fica preso no pescoço e a barra balança, acelerando quando você anda.
  Ver [`AnimatedCape.java`](client/src/main/java/com/solarclient/cosmetics/AnimatedCape.java).

API (rota, autenticação e banco): [`docs/API-COSMETICS.md`](docs/API-COSMETICS.md).

---

## 7. Interface

* **Tema dark** com cor principal customizável (Roxo, Azul, Menta, Pôr do Sol) em
  `Settings → Cor principal`; salva no config.
* **Fonte TTF** (`Sora` por padrão, OFL) rasterizada em um atlas de glifos pelo
  `SolarFont` — nítida em qualquer resolução. Sem o arquivo `.ttf`, cai para a
  fonte do sistema sem quebrar nada. Veja `client/src/main/resources/assets/solarclient/README.md`.
* **Blur** das telas: snapshot do framebuffer → FBO em 1/4 da resolução → upscale
  com filtro linear. Sem shader, sem custo relevante.
* **Animações** com easing em todos os lugares (abertura do menu, interruptores,
  barras, pop da moldura do HUD).

---

## 8. O que ainda precisa ser feito por você

1. **Gerar o `minecraft-1.8.9-mapped.jar`** — não é possível redistribuir o jar
   do Minecraft; os passos estão em [`docs/BUILD.md`](docs/BUILD.md).
2. **Compilar com o OptiFine** baixado (o launcher instala automaticamente se o
   jar do instalador estiver em `cache/`).
3. **Testar os pontos de injeção**: os alvos dos mixins (`setupFog`,
   `hurtCameraEffect`, `canRender`, `handleCustomPayload`, ...) são nomes de
   método do 1.8.9 decompilado — se o seu mapping tiver nomes diferentes, ajuste
   a string em `@Inject(method = "...")`. `docs/ARQUITETURA.md` lista todos.
4. **Registrar um app no Azure** se quiser login Microsoft de verdade
   (`microsoftClientId` em `gradle.properties`).
5. **Ajustar as mensagens do BedWars**: os padrões de kill/bed estão em uma lista
   só, em `SessionTracker`, para você adaptar ao servidor.

---

## 9. Licença e créditos

* Código do client: use como quiser, mas **sem revenda comercial** de client
  com cheat.
* Fontes: Sora (SIL OFL 1.1) — redistribuível com o client.
* OptiFine: licença própria dele (uso pessoal).
* Minecraft: © Mojang. Este projeto **não** distribui o jogo nem assets do jogo.
