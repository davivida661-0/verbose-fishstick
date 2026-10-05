package com.solarclient.bedwars;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import net.minecraft.scoreboard.Scoreboard;
import org.lwjgl.input.Keyboard;

/**
 * <h1>BedWars Overlay</h1>
 *
 * <p>Painel com os recursos da equipe (ferro, ouro, diamante, esmeralda), o
 * tier da forja, o status da cama e as equipes (com as eliminadas marcadas).</p>
 *
 * <p>Os dados vem do {@link BedWarsParser}, que le a sidebar. Nada e enviado
 * ao servidor: e leitura da informacao que o proprio placar ja mostra.</p>
 */
public final class BedWarsOverlay extends HudMod {

    private static final int[] RESOURCE_COLORS = {
            0xD9D9D9, // ferro
            0xFFD24A, // ouro
            0x4FE3F2, // diamante
            0x3EE06B, // esmeralda
    };
    private static final String[] RESOURCE_NAMES = {"Fe", "Ou", "Di", "Es"};

    private final BedWarsInfo info = new BedWarsInfo();
    private long lastParse;

    public BedWarsOverlay() {
        super("BedWars Overlay", "Recursos, forja, camas e equipes",
                ModCategory.BEDWARS, 4, 60, Keyboard.KEY_J);
        values.set("resources", true);
        values.set("forge", true);
        values.set("teams", true);
        values.set("bedStatus", true);
        values.set("interval", 20); // ticks entre leituras da sidebar
    }

    @Override
    public void onTick() {
        if (events().tickCount % Math.max(1, values.getInt("interval", 20)) != 0) {
            return;
        }
        lastParse = System.currentTimeMillis();
    }

    /** Reusa o contador global de ticks sem precisar de campo proprio. */
    private Events.Tick events() {
        return Events.Tick.INSTANCE;
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (event.mc.theWorld == null) {
            return;
        }
        Scoreboard board = event.mc.theWorld.getScoreboard();
        String self = event.mc.getSession() == null ? null : event.mc.getSession().getUsername();
        BedWarsInfo parsed = BedWarsParser.parse(board, self);
        info.clear();
        info.inBedWars = parsed.inBedWars;
        info.iron = parsed.iron;
        info.gold = parsed.gold;
        info.diamond = parsed.diamond;
        info.emerald = parsed.emerald;
        info.forgeTier = parsed.forgeTier;
        info.teams.addAll(parsed.teams);

        if (!info.inBedWars) {
            return;
        }

        int width = 168;
        int height = 18 + (values.getBoolean("resources", true) ? 22 : 0)
                + (values.getBoolean("forge", true) && !info.forgeTier.isEmpty() ? 12 : 0)
                + (values.getBoolean("teams", true) ? 10 * Math.min(info.teams.size(), 4) : 0)
                + 8;

        begin();
        RenderUtils.shadow(0, 0, width, height, 8f, Colors.argb(150, 0, 0, 0), 3);
        RenderUtils.roundedRect(0, 0, width, height, 8f, Colors.argb(150, 10, 12, 20));
        RenderUtils.roundedOutline(0, 0, width, height, 8f, Colors.argb(70, 255, 255, 255));

        SolarFont font = SolarFont.get("Sora", 13, false);
        font.draw("BED WARS", 10, 14, Theme.getAccent());

        int y = 28;
        if (values.getBoolean("resources", true)) {
            for (int i = 0; i < 4; i++) {
                int amount = info.resource(i);
                if (amount < 0) {
                    continue;
                }
                float x = 10 + i * 38;
                RenderUtils.roundedRect(x, y - 8f, 8, 8, 2f, RESOURCE_COLORS[i]);
                font.draw(RESOURCE_NAMES[i], x + 11, y, Theme.TEXT_SECONDARY);
                font.drawRight(String.valueOf(amount), x + 34, y, Theme.TEXT_PRIMARY);
            }
            y += 14;
        }

        if (values.getBoolean("forge", true) && !info.forgeTier.isEmpty()) {
            font.draw("Forja", 10, y, Theme.TEXT_SECONDARY);
            font.draw(info.forgeTier, 10 + font.width("Forja") + 8, y, Theme.WARNING);
            y += 12;
        }

        if (values.getBoolean("teams", true)) {
            int shown = 0;
            for (BedWarsInfo.Team team : info.teams) {
                if (shown >= 4) {
                    break;
                }
                String label = team.name + "  " + team.players + " jogador(es)";
                int color = team.self ? Theme.getAccent()
                        : team.eliminated ? Theme.DANGER : Theme.TEXT_PRIMARY;
                font.draw(label, 10, y, color);

                // status da cama: "ELIMINADA" tem prioridade sobre o estado da cama
                if (team.eliminated) {
                    font.drawRight("ELIMINADA", width - 10, y, Theme.DANGER);
                } else if (values.getBoolean("bedStatus", true)) {
                    boolean bedAlive = BedStatusTracker.get().isAlive(team.name);
                    font.drawRight(bedAlive ? "CAMA OK" : "CAMA QUEBRADA", width - 10, y,
                            bedAlive ? Colors.rgb(0x3E, 0xE0, 0xA4) : Theme.WARNING);
                }
                y += 10;
                shown++;
            }
        }

        setSize(width, height);
        end();
    }
}
