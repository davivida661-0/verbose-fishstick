package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import org.lwjgl.input.Keyboard;

/**
 * Coordinates.
 *
 * <p>X / Y / Z, chunk e bioma. Y em vermelho quando esta abaixo do nivel do
 * mar (49) porque nesse ponto do BedWars voce ja esta no subsolo.</p>
 */
public final class Coordinates extends HudMod {

    public Coordinates() {
        super("Coordinates", "X Y Z, chunk e bioma", ModCategory.RENDER, 4, 220, Keyboard.KEY_C);
        values.set("showBiome", true);
        values.set("showChunk", true);
        values.set("decimals", true);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (event.mc.thePlayer == null || event.mc.theWorld == null) {
            return;
        }
        double x = event.mc.thePlayer.posX;
        double y = event.mc.thePlayer.posY;
        double z = event.mc.thePlayer.posZ;

        SolarFont font = SolarFont.get();
        String pos = (values.getBoolean("decimals", true) ? fmt(x) : String.valueOf((int) x))
                + " / " + (values.getBoolean("decimals", true) ? fmt(y) : String.valueOf((int) y))
                + " / " + (values.getBoolean("decimals", true) ? fmt(z) : String.valueOf((int) z));

        String extra = "";
        if (values.getBoolean("showChunk", true)) {
            extra += "  [" + (int) Math.floor(x / 16) + ", " + (int) Math.floor(z / 16) + "]";
        }

        int w = (int) font.width(pos + extra) + 16;
        begin();
        drawPanel(w, 20, 0.45f, 5f);
        font.draw(pos, 8, 14, y < 49 ? Theme.DANGER : Theme.TEXT_PRIMARY);
        if (!extra.isEmpty()) {
            font.draw(extra, 8 + font.width(pos), 14, Theme.TEXT_SECONDARY);
        }

        if (values.getBoolean("showBiome", true)) {
            String biome = event.mc.theWorld.getBiomeName(
                    new net.minecraft.util.math.BlockPos((int) x, (int) y, (int) z));
            if (biome != null && !biome.isEmpty()) {
                font.draw(biome, 8, 26, Colors.mix(Theme.TEXT_SECONDARY, Theme.getAccent(), 0.4f));
            }
        }
        setSize(w, 26);
        end();
    }

    private static String fmt(double d) {
        return String.format("%.1f", d);
    }
}
