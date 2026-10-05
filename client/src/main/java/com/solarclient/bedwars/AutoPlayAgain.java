package com.solarclient.bedwars;

import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * <h1>Auto Play Again</h1>
 *
 * <p>Envia "/play again" sozinho quando a partida acaba (o servidor mostra
 * "Jogo novamente? [Sim] [Nao]" e o jogador fica com o placar na tela).</p>
 *
 * <p><b>Desligado por padrao</b>: automatizar comando em servidor competitive
 * pode violar as regras. Aqui so envia o comando de chat padrao que qualquer
 * jogador digitaria a mao.</p>
 */
public final class AutoPlayAgain extends Mod {

    private final Minecraft mc = Minecraft.getMinecraft();
    private long sendAt = -1L;

    public AutoPlayAgain() {
        super("Auto Play Again", "Envia /play again no fim da partida (desligado)",
                ModCategory.BEDWARS, 0);
        values.set("command", "/play again");
        values.set("delayMs", 3000);
    }

    @EventBus.Handler
    public void onChat(Events.ChatIncoming event) {
        if (!isEnabled() || event.plain == null) {
            return;
        }
        String text = event.plain.toLowerCase(Locale.ROOT);
        boolean over = text.contains("game over")
                || text.contains("you placed")
                || text.contains("play again")
                || text.contains("jogar novamente");

        if (over) {
            // um pouco de atraso: o placar final ainda esta sendo enviado
            sendAt = System.currentTimeMillis() + values.getInt("delayMs", 3000);
        }
    }

    @Override
    public void onTick() {
        if (sendAt < 0 || mc.thePlayer == null || System.currentTimeMillis() < sendAt) {
            return;
        }
        sendAt = -1;
        mc.thePlayer.sendChatMessage(values.getString("command", "/play again"));
    }
}
