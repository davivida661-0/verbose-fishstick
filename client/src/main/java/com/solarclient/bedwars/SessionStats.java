package com.solarclient.bedwars;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import org.lwjgl.input.Keyboard;

/**
 * Session Counter.
 *
 * <p>Final kills, beds quebrados, vitorias e tempo de sessao. Os numeros vem
 * do {@link SessionTracker}, que escuta o chat do servidor.</p>
 */
public final class SessionStats extends HudMod {

    public SessionStats() {
        super("Session", "Kills, beds, vitorias e tempo de sessao",
                ModCategory.BEDWARS, 4, 90, Keyboard.KEY_L);
        values.set("showTime", true);
        values.set("resetButton", true);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        SessionTracker tracker = SessionTracker.get();
        SolarFont font = SolarFont.get("Sora", 13, false);

        int width = 158;
        int height = values.getBoolean("showTime", true) ? 40 : 30;

        begin();
        drawPanel(width, height, 0.5f, 7f);

        font.draw("Kills", 10, 15, Theme.TEXT_SECONDARY);
        font.drawRight(String.valueOf(tracker.getKills()), width - 10, 15, Theme.DANGER);
        font.draw("Beds", 10, 27, Theme.TEXT_SECONDARY);
        font.drawRight(String.valueOf(tracker.getBeds()), width - 10, 27, Colors.rgb(0x4F, 0xE3, 0xF2));
        font.draw("Wins", 10, height - 5, Theme.TEXT_SECONDARY);
        font.drawRight(String.valueOf(tracker.getWins()), width - 10, height - 5, Theme.SUCCESS);

        if (values.getBoolean("showTime", true)) {
            String time = format(tracker.getSessionMillis());
            RenderUtils.roundedRect(width - 58, 4, 54, 12, 6f, Colors.argb(60, 255, 255, 255));
            font.drawCentered(time, width - 31, 14, Theme.TEXT_PRIMARY);
        }

        setSize(width, height);
        end();
    }

    private static String format(long millis) {
        long seconds = millis / 1000L;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
