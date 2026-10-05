package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Animation;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import com.solarclient.util.CpsTracker;
import org.lwjgl.input.Keyboard;

/**
 * <h1>MOD DE EXEMPLO 2/3 - CPS Counter</h1>
 *
 * <p>Mostra os cliques por segundo do botao esquerdo e do direito, com duas
 * barras que sobem e descem. E leitura pura de input: nenhum pacote e
 * enviado, nenhuma jogabilidade e alterada.</p>
 *
 * <p>Conceitos novos em relacao ao Keystrokes:</p>
 * <ul>
 *     <li>os numeros vem de {@link CpsTracker}, um tracker estatico
 *         (janela deslizante de 1 segundo) compartilhado com o Keystrokes;</li>
 *     <li>a altura das barras usa {@link com.solarclient.ui.Animation} para
 *         subir/descer suavemente em vez de "pular" a cada clique;</li>
 *     <li>o valor maximo da barra e configuravel (boa para quem clica em
 *         cadencia baixa e nao quer a barra sempre cheia).</li>
 * </ul>
 */
public final class CpsCounter extends HudMod {

    private final Animation leftBar = new Animation(0f);
    private final Animation rightBar = new Animation(0f);

    public CpsCounter() {
        super("CPS Counter",
                "Cliques por segundo (LMB / RMB) com barra animada",
                ModCategory.COMBAT,
                4, 100,
                Keyboard.KEY_V);

        values.set("background", true);
        values.set("maxCps", 20);
        values.set("showBars", true);
        values.set("showLastClick", false);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        int left = CpsTracker.left();
        int right = CpsTracker.right();
        int max = Math.max(1, values.getInt("maxCps", 20));

        // Animacoes vao para o valor-alvo; o easing faz o resto
        leftBar.setTarget(Math.min(1f, left / (float) max));
        rightBar.setTarget(Math.min(1f, right / (float) max));
        leftBar.update();
        rightBar.update();

        int accent = Theme.getAccent();
        int w = values.getBoolean("showBars", true) ? 86 : 70;
        int h = values.getBoolean("showLastClick", true) ? 34 : 22;

        begin();
        if (values.getBoolean("background", true)) {
            drawPanel(w + 10, h + 8, 0.45f, 6f);
        }

        // "CPS" + numeros
        SolarFont font = SolarFont.get();
        font.draw("CPS", 6, 14, Theme.TEXT_SECONDARY);
        font.draw(String.valueOf(left), 40, 14, accent);
        font.draw("/", 40 + font.width(String.valueOf(left)) + 3, 14, Theme.TEXT_SECONDARY);
        font.draw(String.valueOf(right), 40 + font.width(String.valueOf(left)) + 9, 14,
                Colors.mix(accent, 0xFFFFFFFF, 0.45f));

        if (values.getBoolean("showBars", true)) {
            drawBar(6, 20, 74, 4f, leftBar.getValue(), accent);
            drawBar(6, 27, 74, 4f, rightBar.getValue(), Colors.mix(accent, 0xFFFFFFFF, 0.45f));
        }

        if (values.getBoolean("showLastClick", false)) {
            font.draw("last: " + CpsTracker.lastLeftPressAgo() + "ms", 6, 34,
                    Colors.argb(140, 255, 255, 255));
            h = 40;
        }

        setSize(w + 10, h + 8);
        end();
    }

    private void drawBar(float x, float y, float w, float h, float value, int color) {
        // trilho
        RenderUtils.roundedRect(x, y, w, h, h / 2f, Colors.argb(70, 255, 255, 255));
        // preenchimento
        float fill = Math.max(0f, Math.min(1f, value)) * w;
        if (fill > 0) {
            RenderUtils.roundedRect(x, y, Math.max(h, fill), h, h / 2f, color);
        }
    }
}
