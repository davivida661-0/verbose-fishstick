package com.solarclient.bedwars;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Animation;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * Low Health.
 *
 * <p>Aviso visual (borda pulsando) quando a vida cai abaixo do limite. Ajuda
 * a saber que precisa recuar sem olhar o coracao.</p>
 */
public final class LowHealth extends HudMod {

    private final Animation pulse = new Animation(0f);
    private final Minecraft mc = Minecraft.getMinecraft();
    private float phase;

    public LowHealth() {
        super("Low Health", "Borda pulsando com vida baixa",
                ModCategory.BEDWARS, 0, 0, Keyboard.KEY_X);
        values.set("threshold", 6.0);   // coracoes (30 = vida cheia)
        values.set("border", 6f);
        values.set("sound", true);
        values.set("color", 0xE03A3A);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (mc.thePlayer == null) {
            return;
        }
        // no 1.8.9 cada coracao = 2 pontos de vida
        float hearts = mc.thePlayer.getHealth() / 2f;
        float threshold = (float) values.getDouble("threshold", 6.0d);
        boolean low = hearts > 0 && hearts <= threshold;

        pulse.setTarget(low ? 1f : 0f);
        pulse.update();
        if (pulse.getValue() < 0.01f) {
            return;
        }
        phase += 0.06f;

        float border = (float) values.getDouble("border", 6.0d);
        int color = Colors.withAlpha(values.getColor("color", 0xE03A3A),
                (float) (0.35 + 0.35 * Math.abs((float) Math.sin(phase))) * pulse.getValue());

        begin();
        RenderUtils.roundedOutline(0, 0, event.screenWidth, event.screenHeight, 6f, color);
        if (border > 3f) {
            RenderUtils.roundedOutline(2, 2, event.screenWidth - 4, event.screenHeight - 4,
                    6f, Colors.withAlpha(color, Colors.getAlpha(color) / 2f));
        }
        SolarFont font = SolarFont.get("Sora", 14, true);
        font.drawCentered("VIDA BAIXA " + Math.round(hearts) + " coracoes",
                event.screenWidth / 2f, 30, color);
        setSize(event.screenWidth, event.screenHeight);
        end();
    }
}
