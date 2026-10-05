package com.solarclient.bedwars;

import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * <h1>Auto GG</h1>
 *
 * <p>Manda "GG" automaticamente quando voce morre no BedWars (o servidor
 * avisa "Você foi eliminado!" / "You were knocked out").</p>
 *
 * <p><b>Por que desligado por padrao:</b> automacao de chat viola as regras
 * da maioria dos servidores competitive e pode custar mute ou ban. O mod
 * existe para quem joga em servidor de treino/amigo. Ligue por conta e risco
 * e confira as regras do servidor.</p>
 *
 * <p>O envio usa {@code EntityPlayerSP.sendChatMessage}, exatamente o que o
 * jogador digitaria: um unico pacote de chat, nada escondido.</p>
 */
public final class AutoGG extends Mod {

    private static final long DELAY_MS = 400L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private long deadAt = -1L;

    public AutoGG() {
        super("Auto GG", "Manda GG ao ser eliminado (desligado por padrao)",
                ModCategory.BEDWARS, 0);
        values.set("message", "GG");
        values.set("delayMs", DELAY_MS);
    }

    @EventBus.Handler
    public void onChat(Events.ChatIncoming event) {
        if (!isEnabled() || event.plain == null) {
            return;
        }
        String text = event.plain.toLowerCase(Locale.ROOT);
        boolean died = text.contains("you were knocked out")
                || text.contains("you were eliminated")
                || text.contains("you died")
                || text.contains("voce foi eliminado")
                || text.contains("final kill:");

        if (died) {
            deadAt = System.currentTimeMillis() + values.getInt("delayMs", (int) DELAY_MS);
        }
    }

    @Override
    public void onTick() {
        if (deadAt < 0 || mc.thePlayer == null) {
            return;
        }
        if (System.currentTimeMillis() < deadAt) {
            return;
        }
        deadAt = -1;
        mc.thePlayer.sendChatMessage(values.getString("message", "GG"));
    }
}
