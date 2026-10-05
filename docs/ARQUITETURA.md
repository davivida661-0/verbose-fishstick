# Arquitetura

Documento curto de "onde mexer em quê". O código tem comentários maiores; aqui
é o mapa.

---

## 1. O fluxo de uma frame

```
Minecraft.runTick()                       <- mixin (TAIL)
      └─> SolarClient.onTick()
            ├─> EventBus.post(Tick)      -> CpsTracker, SessionTracker, mods com @Handler
            └─> ModManager.onTick()      -> tecla (KeyBinding.isPressed) + mod.onTick()

GuiIngame.renderGameOverlay(0)            <- mixin (HEAD)
      └─> SolarClient.onRender2D()
            └─> ModManager.onRender2D() -> cada mod de HUD (begin/scale/end)

EntityRenderer.render(...)                <- mixin (TAIL)
      └─> SolarClient.onRender3D()
            └─> ModManager.onRender3D()  -> NameTags, cosméticos
```

Nada de `if (mod.isEnabled())` espalhado pelo jogo: o `ModManager` só chama
mod ligado, então **mod desligado custa zero** no loop.

---

## 2. Como escrever um mod novo (5 minutos)

1. Crie a classe em `client/src/main/java/com/solarclient/mods/<categoria>/`:

```java
public final class MeuMod extends HudMod {                 // ou extends Mod
    public MeuMod() {
        super("Meu Mod", "descricao que aparece no card",
              ModCategory.COMBAT,                          // categoria do ClickGUI
              4, 4,                                        // x, y initial
              Keyboard.KEY_M);                             // atalho (0 = sem atalho)
        values.set("minhaOpcao", true);                    // default do config
    }

    @Override
    public void onRender2D(Events.Render2D e) {
        begin();                                           // aplica x/y/scale
        drawText("OK", 0, 10, Colors.WHITE);
        setSize(40, 16);                                   // area de arraste
        end();
    }
}
```

2. Registre em `ModRegistry.registerAll(...)`:

```java
manager.register(new MeuMod());
```

3. Se precisar de evento (chat, payload), use o handler no próprio mod:

```java
@EventBus.Handler
public void onChat(Events.ChatIncoming e) { ... }
```

Pronto: ele aparece no ClickGUI, no HUD Editor, tem tecla no menu de
controles do jogo e é salvo no `config.json`.

### Abrir as telas

Os combos (`R+S`, `H+U`, `C+O`) ficam em `GuiShortcuts`, que lê o estado
físico das teclas com `Keyboard.isKeyDown` — independente dos `KeyBinding`
dos mods, por isso funciona com qualquer tela aberta (inclusive o menu
principal). Para mudar um combo:

```java
SolarClient.get().configManager.setChord("clickGuiKey", new int[]{Keyboard.KEY_R, Keyboard.KEY_S});
```

### Mods de comportamento (não-HUD)

`extends Mod` e sobrescreva `onTick()`. Se precisar agachar/correr, use sempre
a API do vanilla (`KeyBinding.setKeyBindState`, `player.setSprinting`) — nunca
um pacote.

### Mods de mixin

Nada de lógica no mixin. Expose um estado estático e consulte do mod:

```java
public final class MeuVisual extends Mod {
    private static MeuVisual instance;
    public MeuVisual() { super(...); instance = this; }
    public static boolean isActive() { return instance != null && instance.isEnabled(); }
}
```

```java
@Inject(method = "algumMetodoDoVanilla", at = @At("HEAD"), cancellable = true)
private void solar$algo(CallbackInfo ci) {
    if (MeuVisual.isActive()) { /* ajuste */ }
}
```

---

## 3. Os 9 pontos de injeção

Todos em `client/src/main/java/com/solarclient/mixin/SolarMixin.java`.
Se o seu mapping do 1.8.9 tiver nomes diferentes, é aqui que se ajusta.

| Mixin | Classe alvo | Método | Para quê |
|-------|-------------|--------|----------|
| `MinecraftMixin` | `Minecraft` | `runTick` (TAIL) | init + tick |
| `MinecraftMixin` | `Minecraft` | `shutdown` (HEAD) | salvar config |
| `GuiIngameMixin` | `GuiIngame` | `renderGameOverlay` (HEAD) | HUD + cancelar sidebar (3) e bossbar (2) |
| `GuiIngameMixin` | `GuiIngame` | `renderGameOverlay` (TAIL) | Tab List por cima da original |
| `EntityRendererMixin` | `EntityRenderer` | `render` (TAIL) | camada 3D |
| `EntityRendererMixin` | `EntityRenderer` | `hurtCameraEffect` (HEAD) | No Hurt Cam |
| `EntityRendererMixin` | `EntityRenderer` | `setupFog` (HEAD) | FullBright |
| `RenderPlayerMixin` | `RenderPlayer` | `render` (HEAD/TAIL) | cosméticos + old sneak |
| `AbstractClientPlayerMixin` | `AbstractClientPlayer` | `getLocationCape` (HEAD) | esconder a capa vanilla |
| `ModelPlayerMixin` | `ModelPlayer` | `setRotationAngles` (TAIL) | blockhit 1.7 |
| `NetHandlerPlayClientMixin` | `NetHandlerPlayClient` | `handleCustomPayload` (HEAD) | sync de cosméticos |
| `NetHandlerPlayClientMixin` | `NetHandlerPlayClient` | `handleChatMessage` (HEAD) | Nick Hider / mods de chat |
| `BlockRenderLayerMixin` | `BlockRenderLayer` | `canRender` (HEAD) | Clear Glass |
| `GuiMainMenuMixin` | `GuiMainMenu` | `drawScreen` (TAIL) | logo + blur no menu principal |
| `RenderGlobalMixin` | `RenderGlobal` | `clearEntities` (HEAD) | limpar cache ao sair do mundo |

O registro está em `client/src/main/resources/solar.mixins.json`.

---

## 4. Renderização 2D

* `RenderUtils` — retângulo arredondado (fan de triângulos), gradiente, sombra,
  círculo, linha, scissor. Sem textura, sem `drawText` do vanilla.
* `SolarFont` — fonte TTF rasterizada com AWT em um atlas de glifos, desenhada
  em quads. Cache por `(família, tamanho, bold)`.
* `Blur` — snapshot do framebuffer → FBO 1/4 → upscale linear. Cai para um
  overlay escuro se o OpenGL não deixar (nunca quebra a tela).
* `Theme` / `Colors` — paleta e cor de destaque customizável.

Coordenadas: tudo em **pixels já escalados** pelo `ScaledResolution` (o que o
`GuiIngame` entrega). O `Display.getWidth()` só é usado para converter no
scissor.

---

## 5. Configuração

Arquivo único: `solar-client/config.json`.

```json
{
  "client":  { "name": "Solar Client", "version": "0.1.0", "accent": "PURPLE" },
  "hud":     { "snap": true, "grid": 5, "accent": "PURPLE" },
  "session": { "kills": 12, "beds": 4, "wins": 1 },
  "mods": {
    "Keystrokes": {
      "enabled": true, "x": 4, "y": 4, "scale": 1.0,
      "values": { "colorMode": 0, "cellSize": 26, "showCps": true }
    }
  }
}
```

`ModSettings` é um `JsonObject` por mod: criar uma opção nova é só
`values.set("novaOpcao", ...)`, sem migração de schema. A tecla também mora
aqui (`"key"`), e por isso o `ConfigManager` roda **antes** das `KeyBinding`.

---

## 5b. BedWars: onde vêm os dados

| Informação | Origem |
|------------|--------|
| Recursos, forja, equipes | `BedWarsParser` lendo a sidebar (1x por intervalo) |
| Cama quebrada / intacta | `BedStatusTracker` lendo o kill feed do chat |
| Kills / beds / wins | `SessionTracker` lendo o chat (frases centralizadas) |
| Jogador invisível perto | varredura de entidades a cada tick, só avisa |

Todos os padrões de frase ficam em listas estáticas — se o servidor escrever
diferente, muda-se **um** lugar, não a lógica.

## 6. Cosméticos

```
CosmeticManager
   ├── catálogo: id -> Cosmetic        (AnimatedCape, ... em buildCatalog)
   ├── loadout: uuid -> (tipo -> id)   (solar-client/cosmetics.json)
   ├── API:    loadout -> servidor     (a cada 5 min, thread daemon)
   └── rede:   payload "solar|cosmetics" para os outros clients
```

Para adicionar um cosmético:

1. textura em `assets/solarclient/textures/cosmetics/...`;
2. classe que estende `Cosmetic` e implementa `render(EntityPlayer, float)`
   (use `AnimatedCape` como modelo de malha/OpenGL);
3. `manager.register(new MeuCosmetico("id", Rarity.EPIC, "Nome"))`;
4. registre o item no banco (`api/sql/schema.sql`).

O preview 3D da GUI reusa o `RenderPlayer` do jogo com o jogador real — por
isso ela só mostra o modelo com um mundo carregado.

---

## 7. Performance: o que fazer antes de criar um mod novo

* Nada de alocar em `onRender2D` (crie listas/cores fora do loop).
* `CpsTracker` escuta `Render2D` (e não `Tick`) para contar CPS com precisão.
* Fontes e texturas têm cache; não carregue por frame.
* `EventBus` faz reflection **uma vez** (no `register`).
* Mod desligado = zero chamadas (o `ModManager` filtra).

---

## 8. Onde NÃO mexer

* `SolarClient.init()` — a ordem importa (config → mods → keys → eventos).
* O canal `solar|cosmetics` — payload de tamanho limitado (32 KB) e só
  informação cosmética; nada de gameplay.
* `ModSettings` — é a base do config; mudar a forma quebra os JSONs do usuário.
