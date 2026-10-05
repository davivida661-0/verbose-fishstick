package com.solarclient.bedwars;

import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.util.ChatUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * <h1>Status das camas (alive / broken)</h1>
 *
 * <p>A sidebar do BedWars nao mostra quem ainda tem cama - essa informacao vem
 * do <b>kill feed</b>. Este tracker escuta o chat e marca a equipe cujo anuncio
 * apareceu:</p>
 *
 * <pre>
 *   "Voce destruiu a cama do Vermelho!"   -&gt; marca Vermelho como quebrada
 *   "A cama do Azul foi destruida"          -&gt; idem
 *   "Voce foi destruido"                    -&gt; (a sua cama: handled pelo overlay)
 * </pre>
 *
 * <p><b>Por que as frases estao numa lista so:</b> cada servidor escreve o
 * texto do seu jeito. Se o seu nao bater, e so acrescentar o padrao em
 * {@link #BED_MARKERS} - nenhuma outra classe muda.</p>
 *
 * <p>O tracker e reiniciado quando o placar some (fim da partida), o que
 * acontece sozinho porque as equipes desaparecem do {@link BedWarsInfo}.</p>
 */
public final class BedStatusTracker {

    /** Palavras que indicam quebra de cama no kill feed. */
    private static final String[] BED_MARKERS = {
            "bed was destroyed", "destroyed", "destruiu a cama", "cama foi destruida",
            "bed got destroyed", "broke the bed",
    };

    private static final BedStatusTracker INSTANCE = new BedStatusTracker();

    /** Equipes com cama ja quebrada (normalizado em minusculas, sem cor). */
    private final Set<String> broken = new HashSet<>();

    private BedStatusTracker() {
    }

    public static BedStatusTracker get() {
        return INSTANCE;
    }

    public static void register(EventBus bus) {
        bus.register(INSTANCE);
    }

    @EventBus.Handler
    public void onChat(Events.ChatIncoming event) {
        if (event.plain == null) {
            return;
        }
        String text = ChatUtils.strip(event.plain).toLowerCase(Locale.ROOT);
        if (!contains(text, BED_MARKERS)) {
            return;
        }
        String team = extractTeam(text);
        if (team != null) {
            broken.add(team);
        }
    }

    /** A cama dessa equipe ainda esta de pe? */
    public boolean isAlive(String team) {
        return team != null && !broken.contains(normalize(team));
    }

    /** Todas as equipes marcadas (para o overlay). */
    public Map<String, Boolean> statusOf(Iterable<BedWarsInfo.Team> teams) {
        Map<String, Boolean> status = new HashMap<>();
        for (BedWarsInfo.Team team : teams) {
            status.put(team.name, !broken.contains(normalize(team.name)));
        }
        return status;
    }

    /** Limpa o cache (troca de partida). */
    public void reset() {
        broken.clear();
    }

    // ------------------------------------------------------------------ interno
    private static boolean contains(String text, String[] markers) {
        for (String marker : markers) {
            if (text.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Descobre de quem e a cama na frase.
     *
     * <p>Padroes suportados (minusculas, sem codigo de cor):</p>
     * <pre>
     *   "voce destruiu a cama do vermelho"  -&gt; "vermelho"
     *   "a cama do azul foi destruida"      -&gt; "azul"
     *   "you destroyed red's bed"           -&gt; "red"
     * </pre>
     *
     * @return nome da equipe em minusculas, ou null se nao der para saber
     */
    static String extractTeam(String text) {
        // "a cama do <equipe>"
        int cama = text.indexOf("cama do ");
        if (cama >= 0) {
            return firstWord(text.substring(cama + "cama do ".length()));
        }
        // "you destroyed <equipe>'s bed"
        int destroyed = text.indexOf("destroyed ");
        if (destroyed >= 0) {
            String rest = text.substring(destroyed + "destroyed ".length());
            int apostrophe = rest.indexOf("'s");
            if (apostrophe > 0) {
                return rest.substring(0, apostrophe).trim();
            }
            return firstWord(rest);
        }
        return null;
    }

    private static String firstWord(String text) {
        String clean = text.replaceAll("[^a-z0-9 ]", " ").trim();
        int space = clean.indexOf(' ');
        String word = space > 0 ? clean.substring(0, space) : clean;
        return word.isEmpty() ? null : word;
    }

    private static String normalize(String team) {
        return ChatUtils.strip(team).trim().toLowerCase(Locale.ROOT);
    }
}
