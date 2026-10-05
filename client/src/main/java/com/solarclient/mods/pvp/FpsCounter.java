package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Theme;
import org.lwjgl.input.Keyboard;

/**
 * FPS Counter.
 *
 * <p>Conta os frames sozinho em vez de ler um contador interno do jogo: assim
 * o numero e sempre o FPS real de render (nao o "tick rate" de 20).</p>
 *
 * <p>Mostra o valor grande, a media dos ultimos 5 segundos e a minima (que e o
 * que dói em PvP: o "1% low").</p>
 */
public final class FpsCounter extends HudMod {

    private int frames;
    private long windowStart;
    private int fps;
    private int average;
    private int min = Integer.MAX_VALUE;
    private int windows;

    public FpsCounter() {
        super("FPS", "Contador de FPS com media e minimo", ModCategory.COMBAT, 4, 4, Keyboard.KEY_F6);
        values.set("showAverage", true);
        values.set("showMin", true);
        values.set("compact", false);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        long now = System.currentTimeMillis();
        frames++;

        if (now - windowStart >= 1000L) {
            fps = frames;
            frames = 0;
            windowStart = now;
            windows++;

            // media movel simples: mantem o ultimo valor como 70% e o novo 30%
            average = average == 0 ? fps : (int) (average * 0.7f + fps * 0.3f);
            min = Math.min(min, fps);
            if (windows % 30 == 0) {
                // a cada 30s o minimo e recalculado, senao ficaria preso no pior
                // FPS de uma janela de lag isolada
                min = fps;
            }
        }

        int color = fps >= 60 ? Theme.SUCCESS : (fps >= 30 ? Theme.WARNING : Theme.DANGER);

        begin();
        drawPanel(values.getBoolean("compact", false) ? 52 : 84, 24, 0.45f, 6f);

        com.solarclient.ui.SolarFont font = com.solarclient.ui.SolarFont.get();
        font.draw(String.valueOf(fps), 8, 17, color);
        if (values.getBoolean("showAverage", true)) {
            font.draw("avg " + average, 34, 17, Theme.TEXT_SECONDARY);
        }
        if (values.getBoolean("showMin", true) && !values.getBoolean("compact", false)) {
            font.draw("min " + (min == Integer.MAX_VALUE ? fps : min), 34, 27,
                    Theme.TEXT_SECONDARY);
        }

        setSize(values.getBoolean("compact", false) ? 52 : 84, 24);
        end();
    }
}
