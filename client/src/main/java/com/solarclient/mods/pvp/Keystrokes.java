package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import com.solarclient.util.CpsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Keyboard;

/**
 * <h1>MOD DE EXEMPLO 1/3 - Keystrokes</h1>
 *
 * <p>Desenha o teclado virtual (W A S D), os botoes do mouse (LMB/RMB) e a
 * barra de espaco. E o mod de HUD mais usado em PvP porque da para ler a
 * situacao do combate sem tirar os olhos do adversario: "ele esta andando para
 * tras e batendo".</p>
 *
 * <p>Como ele foi construido (e o mesmo caminho de qualquer outro mod):</p>
 * <ol>
 *     <li><b>extends {@link HudMod}</b> - ganha posicao/escala e o
 *         {@code begin()}/{@code end()};</li>
 *     <li><b>Construtor</b> - nome, descricao, categoria, posicao inicial e a
 *         tecla de atalho. Os valores dentro de {@code values.set(...)} sao os
 *         <i>defaults</i>: so valem se o usuario nunca salvou esse mod;</li>
 *     <li><b>{@link #onRender2D(Events.Render2D)}</b> - unico metodo de
 *         desenho, recebe largura/altura ja escaladas e o mouse;</li>
 *     <li><b>{@code setSize(w, h)}</b> no final - e o que o HUD Editor usa
 *         para saber a area de arraste.</li>
 * </ol>
 *
 * <p><b>Configuracoes:</b> background, outline, colorMode (0 = cor do tema,
 * 1 = rainbow, 2 = cor livre), customColor, showCps e cellSize.</p>
 */
public final class Keystrokes extends HudMod {

    private static final int CELL = 26;
    private static final int GAP = 3;
    /** Duracao do "flash" branco ao pressionar a tecla. */
    private static final long FLASH_MS = 130L;

    private final Minecraft mc = Minecraft.getMinecraft();

    // Um Flash por tecla: guarda o instante do ultimo "acabou de apertar".
    private final Flash forward = new Flash();
    private final Flash left = new Flash();
    private final Flash back = new Flash();
    private final Flash right = new Flash();
    private final Flash jump = new Flash();
    private final Flash attack = new Flash();
    private final Flash use = new Flash();

    public Keystrokes() {
        super("Keystrokes",
                "Teclado virtual (WASD + mouse + espaco) com flash ao pressionar",
                ModCategory.COMBAT,
                4, 4,
                Keyboard.KEY_R);

        // ---- defaults (o config.json sobrescreve quando existir) ----
        values.set("background", true);
        values.set("outline", true);
        values.set("colorMode", 0);      // 0 accent | 1 rainbow | 2 custom
        values.set("customColor", 0xFF7C5CFF);
        values.set("showCps", true);
        values.set("cellSize", CELL);
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void onTick() {
        GameSettings key = mc.gameSettings;
        forward.update(key.keyBindForward.isKeyDown());
        left.update(key.keyBindLeft.isKeyDown());
        back.update(key.keyBindBack.isKeyDown());
        right.update(key.keyBindRight.isKeyDown());
        jump.update(key.keyBindJump.isKeyDown());
        attack.update(key.keyBindAttack.isKeyDown());
        use.update(key.keyBindUseItem.isKeyDown());
    }

    // ------------------------------------------------------------------ render
    @Override
    public void onRender2D(Events.Render2D event) {
        GameSettings key = mc.gameSettings;
        int cell = values.getInt("cellSize", CELL);
        int width = cell * 3 + GAP * 2;
        int height = cell * 3 + GAP * 2;

        int accent = keyColor(event.partialTicks);
        int idle = Colors.argb(150, 30, 34, 48);
        int idleText = Theme.TEXT_SECONDARY;

        begin();

        if (values.getBoolean("background", true)) {
            drawPanel(width + 8, height + 8, 0.45f, 8f);
        }

        // ---------------------------------------------------------- linha 1: W
        drawKey(cell + GAP, 0, cell, cell, "W",
                key.keyBindForward.isKeyDown(), accent, idle, idleText, forward);

        // ------------------------------------------------ linha 2: A S D
        drawKey(0, cell + GAP, cell, cell, "A",
                key.keyBindLeft.isKeyDown(), accent, idle, idleText, left);
        drawKey(cell + GAP, cell + GAP, cell, cell, "S",
                key.keyBindBack.isKeyDown(), accent, idle, idleText, back);
        drawKey((cell + GAP) * 2, cell + GAP, cell, cell, "D",
                key.keyBindRight.isKeyDown(), accent, idle, idleText, right);

        // -------------------------------------- linha 3: mouse + espaco
        drawKey(0, (cell + GAP) * 2, cell, cell, "LMB",
                CpsTracker.isLeftDown(), accent, idle, idleText, attack);
        drawKey((cell + GAP) * 2, (cell + GAP) * 2, cell, cell, "RMB",
                CpsTracker.isRightDown(), accent, idle, idleText, use);

        // A barra de espaco ocupa a celula do meio da ultima linha
        drawKey(cell + GAP, (cell + GAP) * 2, cell, cell, "SPACE",
                key.keyBindJump.isKeyDown(), accent, idle, idleText, jump);

        // CPS dentro dos botoes do mouse
        if (values.getBoolean("showCps", true)) {
            SolarFont font = SolarFont.get();
            font.drawCentered(String.valueOf(CpsTracker.left()),
                    cell / 2f, (cell + GAP) * 2 + cell - 6f, Colors.argb(170, 255, 255, 255));
            font.drawCentered(String.valueOf(CpsTracker.right()),
                    (cell + GAP) * 2 + cell / 2f, (cell + GAP) * 2 + cell - 6f,
                    Colors.argb(170, 255, 255, 255));
        }

        // Area de arraste do HUD Editor
        setSize(width + 8, height + 8);
        end();
    }

    // ------------------------------------------------------------------ helpers
    private void drawKey(float x, float y, float w, float h, String label,
                        boolean pressed, int accent, int idle, int idleText, Flash flash) {
        // Flash: 130ms apos pressionar a cor vai do accent para um branco claro
        float flashValue = flash.value();
        int background = pressed ? Colors.mix(accent, 0xFFFFFFFF, flashValue * 0.6f) : idle;

        RenderUtils.roundedRect(x, y, w, h, 5f, background);
        if (values.getBoolean("outline", true)) {
            RenderUtils.roundedOutline(x, y, w, h, 5f, Colors.argb(60, 255, 255, 255));
        }

        // Fonte menor quando o rotulo e longo (SPACE precisa caber)
        SolarFont font = label.length() > 3 ? SolarFont.get("Sora", 10, false) : SolarFont.get();
        font.drawCentered(label, x + w / 2f, y + h / 2f + 4f, pressed ? 0xFF0B0C12 : idleText);
    }

    /** Cor do estado "pressionado": tema, rainbow ou cor escolhida. */
    private int keyColor(float partialTicks) {
        int mode = values.getInt("colorMode", 0);
        if (mode == 1) {
            return Colors.rainbow(partialTicks / 40f, 0.12f);
        }
        if (mode == 2) {
            return values.getColor("customColor", 0xFF7C5CFF);
        }
        return Theme.getAccent();
    }

    /**
     * Guarda o estado do botao entre ticks para saber quando ele "acabou de
     * ser pressionado" e iniciar o flash.
     */
    private static final class Flash {
        private boolean down;
        private long lastPress;

        void update(boolean isDown) {
            if (isDown && !down) {
                lastPress = System.currentTimeMillis();
            }
            down = isDown;
        }

        /** 1.0 no instante do clique, caindo linearmente ate 0 em {@link #FLASH_MS}. */
        float value() {
            if (lastPress == 0) {
                return 0f;
            }
            float v = 1f - (System.currentTimeMillis() - lastPress) / (float) FLASH_MS;
            return v < 0f ? 0f : v;
        }
    }
}
