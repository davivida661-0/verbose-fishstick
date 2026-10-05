package com.solarclient.mods.render;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import com.solarclient.util.ChatUtils;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Scoreboard Mod.
 *
 * <p>Substitui a sidebar do vanilla por uma versao no estilo client: fundo
 * escuro arredondado, titulo em destaque e linhas ordenadas por pontuacao.
 * O vanilla e cancelado pelo mixin do {@code GuiIngame} (tipo 3) e este mod
 * desenha por cima.</p>
 */
public final class ScoreboardMod extends HudMod {

    private static ScoreboardMod instance;

    public ScoreboardMod() {
        super("Scoreboard", "Sidebar customizada (titulo + linhas ordenadas)",
                ModCategory.RENDER, 0, 0, Keyboard.KEY_S);
        instance = this;
        values.set("maxLines", 15);
        values.set("titleAccent", true);
        values.set("rightAlign", true);
    }

    public static ScoreboardMod get() {
        return instance;
    }

    /**
     * Desenha a sidebar customizada. Chamado pelo mixin quando o mod esta
     * ligado (o vanilla foi cancelado).
     */
    public static void renderReplacement(Events.Render2D event) {
        ScoreboardMod mod = instance;
        if (mod == null || !mod.isEnabled() || event.mc.theWorld == null) {
            return;
        }
        mod.draw(event);
    }

    private void draw(Events.Render2D event) {
        Scoreboard board = event.mc.theWorld.getScoreboard();
        if (board == null) {
            return;
        }
        // slot 1 = sidebar (a que fica no lado direito da tela)
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return;
        }

        String title = ChatUtils.getUnformattedText(objective.getDisplayName());
        List<Score> scores = new ArrayList<>(board.getScores(objective));
        Collections.sort(scores, new Comparator<Score>() {
            @Override
            public int compare(Score a, Score b) {
                return Integer.compare(b.getScorePoints(), a.getScorePoints());
            }
        });

        int maxLines = Math.max(1, values.getInt("maxLines", 15));
        if (scores.size() > maxLines) {
            scores = scores.subList(0, maxLines);
        }

        SolarFont font = SolarFont.get();
        int rowHeight = 11;
        int width = 180;
        int height = 20 + rowHeight * scores.size();

        // canto superior direito, com a mesma margem da vanilla
        int x = event.screenWidth - width - 4;
        int y = 4;

        begin();
        RenderUtils.shadow(x, y, width, height, 8f, Colors.argb(160, 0, 0, 0), 3);
        RenderUtils.roundedRect(x, y, width, height, 8f, Colors.argb(150, 10, 12, 20));
        RenderUtils.roundedOutline(x, y, width, height, 8f, Colors.argb(70, 255, 255, 255));

        // cabecalho
        RenderUtils.roundedRect(x, y, width, 18, 8f, Theme.getAccentSoft());
        font.drawCentered(title, x + width / 2f, y + 13,
                values.getBoolean("titleAccent", true) ? Theme.getAccent() : Theme.TEXT_PRIMARY);

        int lineY = y + 30;
        for (Score score : scores) {
            String line = ChatUtils.strip(score.getScoreName());
            // o nome costuma vir "Jogador: 12"
            int colon = line.lastIndexOf(':');
            if (colon > 0) {
                line = line.substring(0, colon) + ": " + line.substring(colon + 1).trim();
            }
            if (values.getBoolean("rightAlign", true)) {
                font.drawRight(line, x + width - 8, lineY, Theme.TEXT_PRIMARY);
            } else {
                font.draw(line, x + 8, lineY, Theme.TEXT_PRIMARY);
            }
            lineY += rowHeight;
        }

        setSize(width, height);
        end();
    }
}
