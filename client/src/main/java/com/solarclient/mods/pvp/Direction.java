package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;

/**
 * Direction (bussola).
 *
 * <p>Mostra para onde o jogador esta olhando (N/L/S/O) e o yaw em graus. Em
 * BedWars ajuda a nao errar o angulo da fireball.</p>
 */
public final class Direction extends HudMod {

    public Direction() {
        super("Direction", "Direcao (N/L/S/O) e yaw", ModCategory.RENDER, 4, 250, 0);
        values.set("showYaw", true);
        values.set("showPitch", false);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (event.mc.thePlayer == null) {
            return;
        }
        float yaw = event.mc.thePlayer.rotationYaw;
        float pitch = event.mc.thePlayer.rotationPitch;

        String cardinal = cardinal(yaw);
        SolarFont font = SolarFont.get();
        String text = cardinal;
        if (values.getBoolean("showYaw", true)) {
            text += "  " + Math.round(normalize(yaw)) + "°";
        }
        if (values.getBoolean("showPitch", true)) {
            text += "  " + Math.round(normalize(pitch)) + "°";
        }

        int w = (int) font.width(text) + 16;
        begin();
        drawPanel(w, 20, 0.45f, 5f);
        font.draw(cardinal, 8, 14, Theme.getAccent());
        if (text.length() > cardinal.length()) {
            font.draw(text.substring(cardinal.length()), 8 + font.width(cardinal), 14,
                    Theme.TEXT_SECONDARY);
        }
        setSize(w, 20);
        end();
    }

    /** -Z e Norte (igual ao mapa do Minecraft). */
    public static String cardinal(float yaw) {
        float y = normalize(yaw);
        if (y >= 45 && y < 135) {
            return "W";   // oeste
        }
        if (y >= 135 && y < 225) {
            return "N";
        }
        if (y >= 225 && y < 315) {
            return "E";
        }
        return "S";
    }

    private static float normalize(float angle) {
        float a = angle % 360f;
        return a < 0 ? a + 360f : a;
    }
}
