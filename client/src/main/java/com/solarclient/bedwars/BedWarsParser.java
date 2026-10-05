package com.solarclient.bedwars;

import com.solarclient.util.ChatUtils;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreTeam;

import java.util.Collection;

/**
 * <h1>Leitor de BedWars</h1>
 *
 * <p>Le a sidebar (o placar do lado direito) e transforma em
 * {@link BedWarsInfo}. O formato do Hypixel e este:</p>
 *
 * <pre>
 *   §lBED WARS          <- titulo
 *   §7Map: §fDowntown
 *   §7Your Team: §cRed §7- §f1
 *   §7Diamonds: §b3
 *   §7Iron: §f24
 *   §7Gold: §e5
 *   §7Emeralds: §a0
 *   §7Forged: §a12      <- total forjado
 *   §7Forge Tier: §6II
 *   §7§lRed §7: §f1
 * </pre>
 *
 * <p>Como cada servidor (e cada modo) escreve de um jeito, o parser trabalha
 * por palavra-chave em minusculas e ignora o que nao reconhecer. Se o seu
 * servidor escrever "Emerald" no singular, ele pega do mesmo jeito.</p>
 */
public final class BedWarsParser {

    private BedWarsParser() {
    }

    public static BedWarsInfo parse(Scoreboard board, String selfName) {
        BedWarsInfo info = new BedWarsInfo();
        if (board == null) {
            return info;
        }

        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return info;
        }

        String title = ChatUtils.getUnformattedText(objective.getDisplayName());
        info.inBedWars = title.toUpperCase().contains("BED WARS")
                || title.toUpperCase().contains("BEDWARS");

        Collection<Score> scores = board.getScores(objective);
        for (Score score : scores) {
            String line = ChatUtils.strip(score.getScoreName());
            String lower = line.toLowerCase();

            if (lower.startsWith("iron:")) {
                info.iron = number(lower.substring("iron:".length()));
            } else if (lower.startsWith("gold:")) {
                info.gold = number(lower.substring("gold:".length()));
            } else if (lower.startsWith("diamond")) {
                info.diamond = number(line.substring(lower.indexOf(':') + 1));
            } else if (lower.startsWith("emerald")) {
                info.emerald = number(line.substring(lower.indexOf(':') + 1));
            } else if (lower.contains("forge tier")) {
                info.forgeTier = line.substring(line.indexOf(':') + 1).trim();
            }
        }

        // equipes pela tab list de times do placar
        Collection<ScoreTeam> scoreTeams = board.getTeams();
        for (ScoreTeam team : scoreTeams) {
            BedWarsInfo.Team t = new BedWarsInfo.Team();
            t.name = ChatUtils.getUnformattedText(team.getDisplayName());
            t.players = team.getMembership().size();
            t.eliminated = t.players == 0;
            t.self = selfName != null && t.name.equals(selfName);
            if (t.self) {
                String prefix = ChatUtils.getUnformattedText(team.getPrefix());
                if (prefix.startsWith(t.name)) {
                    t.name = prefix;
                }
            }
            info.teams.add(t);
        }
        return info;
    }

    private static int number(String text) {
        try {
            return Integer.parseInt(text.trim().replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
