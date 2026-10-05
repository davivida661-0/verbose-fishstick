package com.solarclient.bedwars;

import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * <h1>Auto Tip</h1>
 *
 * <p>Manda "/tip all" no fim da partida, para o jogador que esta dando mais
 * gorjeta. <b>Desligado por padrao</b> (mesma razao do Auto GG).</p>
 */
public final class AutoTip extends Mod {

    private final Minecraft mc = Minecraft.getMinecraft();
    private long sendAt = -1L;

    public AutoTip() {
        super("Auto Tip", "Manda /tip all no fim da partida (desligado)",
                ModCategory.BEDWARS, 0);
        values.set("command", "/tip all");
        values.set("delayMs", 4500);
    }

    @EventBus.Handler
    public void onChat(Events.ChatIncoming event) {
        if (!isEnabled() || event.plain == null) {
            return;
        }
        String text = event.plain.toLowerCase(Locale.ROOT);
        if (text.contains("you placed") || text.contains("game over")) {
            sendAt = System.currentTimeMillis() + values.getInt("delayMs", 4500);
        }
    }

    @Override
    public void onTick() {
        if (sendAt < 0 || mc.thePlayer == null || System.currentTimeMillis() < sendAt) {
            return;
        }
        sendAt = -1;
        mc.thePlayer.sendChatMessage(values.getString("command", "/tip all"));
    }
}
