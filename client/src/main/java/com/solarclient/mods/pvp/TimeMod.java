package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;

import java.util.Calendar;
/**
 * Time / Day.
 *
 * <p>Mostra o tempo do mundo (0-24000) se o servidor deixa alterar
 * (relampago/desafio) e o horario real do jogador. Em BedWars saber se o sol
 * vai nascer ajuda a escolher o lado da spawn.</p>
 */
public final class TimeMod extends HudMod {

    public TimeMod() {
        super("Time", "Tempo do mundo e horario real", ModCategory.RENDER, 4, 280, 0);
        values.set("showRealTime", true);
        values.set("showWorldTime", true);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        SolarFont font = SolarFont.get();
        String world = "";
        if (values.getBoolean("showWorldTime", true) && event.mc.theWorld != null) {
            long time = event.mc.theWorld.getWorldTime() % 24000L;
            world = String.format("dia %02d:%02d", time / 1000L, (time % 1000L) * 6L / 100L);
        }

        String real = "";
        if (values.getBoolean("showRealTime", true)) {
            Calendar now = Calendar.getInstance();
            real = String.format("%02d:%02d:%02d",
                    now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        }

        String text = real.isEmpty() ? world : (world.isEmpty() ? real : world + "  " + real);
        if (text.isEmpty()) {
            return;
        }

        int w = (int) font.width(text) + 16;
        begin();
        drawPanel(w, 20, 0.45f, 5f);
        font.draw(text, 8, 14, Colors.mix(Theme.TEXT_PRIMARY, Theme.getAccent(), 0.25f));
        setSize(w, 20);
        end();
    }
}
