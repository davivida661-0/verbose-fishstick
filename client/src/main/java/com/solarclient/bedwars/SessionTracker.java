package com.solarclient.bedwars;

import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.util.ChatUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <h1>Contador de sessao</h1>
 *
 * <p>Final kills, beds quebrados e vitorias da sessao atual. Os dados vem do
 * chat do servidor (que e a unica fonte de resultado no cliente) e ficam
 * salvos no {@code config.json} para nao sumir quando o cliente fechar.</p>
 *
 * <p><b>As frases sao centralizadas aqui</b> de proposito: cada servidor
 * escreve o kill feed do seu jeito, entao se o seu nao bater, e so ajustar
 * esta lista - nenhuma outra classe precisa mudar.</p>
 */
public final class SessionTracker {

    // Frases do Hypixel BedWars (minusculas, sem codigo de cor)
    private static final List<String> KILL_PATTERNS = Arrays.asList(
            "you knocked", "you killed", "final kill: you");
    private static final List<String> BED_PATTERNS = Arrays.asList(
            "you destroyed", "bed broken by you", "you broke");
    private static final List<String> WIN_PATTERNS = Arrays.asList(
            "you placed #1", "placed #1", "victory!", "wins: 1");

    private static final SessionTracker INSTANCE = new SessionTracker();

    private int kills;
    private int beds;
    private int wins;
    private long sessionStart = System.currentTimeMillis();

    private SessionTracker() {
    }

    public static SessionTracker get() {
        return INSTANCE;
    }

    public int getKills() {
        return kills;
    }

    public int getBeds() {
        return beds;
    }

    public int getWins() {
        return wins;
    }

    public long getSessionMillis() {
        return System.currentTimeMillis() - sessionStart;
    }

    public void reset() {
        kills = 0;
        beds = 0;
        wins = 0;
        sessionStart = System.currentTimeMillis();
    }

    public void load(int kills, int beds, int wins) {
        this.kills = kills;
        this.beds = beds;
        this.wins = wins;
    }

    /** Registra o listener (o SolarClient ja registra os mods no bus). */
    public static void register(EventBus bus) {
        bus.register(INSTANCE);
    }

    @EventBus.Handler
    public void onChat(Events.ChatIncoming event) {
        if (event.plain == null) {
            return;
        }
        String text = ChatUtils.strip(event.plain).toLowerCase(Locale.ROOT);
        if (matches(text, KILL_PATTERNS)) {
            kills++;
        }
        if (matches(text, BED_PATTERNS)) {
            beds++;
        }
        if (matches(text, WIN_PATTERNS)) {
            wins++;
        }
    }

    private static boolean matches(String text, List<String> patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
