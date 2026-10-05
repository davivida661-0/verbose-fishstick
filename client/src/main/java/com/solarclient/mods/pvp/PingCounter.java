package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * Ping Counter.
 *
 * <p>Mostra o ping do servidor em ms com uma cor que piora conforme o valor
 * (verde &lt; 80, amarelo &lt; 150, vermelho acima). E leitura da informacao
 * que a propria tela de debug (F3) mostra - nada de packet.</p>
 *
 * <p>A latencia vem da tab list do proprio cliente
 * ({@code getNetHandler().getPlayerInfo(nome).getLatency()}), que e
 * exatamente o valor exibido pelo ping do vanilla.</p>
 */
public final class PingCounter extends HudMod {

    private static final int[] PING_COLORS = {
            0xFF3EE0A4, // &lt; 50  excelente
            0xFFA8E05F, // &lt; 90  bom
            0xFFFFC857, // &lt; 150  ruim
            0xFFFF5C5C, // &gt;= 150 ruim demais
    };

    private final Minecraft mc = Minecraft.getMinecraft();
    private int ping = -1;

    public PingCounter() {
        super("Ping", "Latencia do servidor em ms", ModCategory.COMBAT, 4, 32, Keyboard.KEY_P);
        values.set("showBar", true);
        values.set("showMs", true);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null) {
            ping = -1;
            return;
        }
        try {
            // 1.8.9: a latencia vem da tab list mantida pelo cliente, que e
            // exatamente o numero que o ping do vanilla mostra.
            ping = mc.getNetHandler() == null
                    ? -1
                    : mc.getNetHandler().getPlayerInfo(mc.getSession().getUsername()).getLatency();
            if (ping < 0) {
                ping = 0;
            }
        } catch (Exception e) {
            ping = -1; // singleplayer / sem tab list
        }
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (ping < 0) {
            return;
        }
        int color = colorFor(ping);
        int w = 66;
        int h = 22;

        begin();
        drawPanel(w + 8, h + 6, 0.45f, 6f);

        SolarFont font = SolarFont.get();
        font.draw(String.valueOf(ping), 6, 16, color);
        if (values.getBoolean("showMs", true)) {
            font.draw("ms", 6 + font.width(String.valueOf(ping)) + 3, 16, Theme.TEXT_SECONDARY);
        }

        if (values.getBoolean("showBar", true)) {
            // barra de 0 a 300ms
            float ratio = Math.min(1f, ping / 300f);
            RenderUtils.roundedRect(w - 22, 11, 22, 4f, 2f, Colors.argb(70, 255, 255, 255));
            RenderUtils.roundedRect(w - 22, 11, Math.max(4f, 22f * ratio), 4f, 2f, color);
        }

        setSize(w + 8, h + 6);
        end();
    }

    private static int colorFor(int ping) {
        if (ping < 50) {
            return PING_COLORS[0];
        }
        if (ping < 90) {
            return PING_COLORS[1];
        }
        if (ping < 150) {
            return PING_COLORS[2];
        }
        return PING_COLORS[3];
    }
}
