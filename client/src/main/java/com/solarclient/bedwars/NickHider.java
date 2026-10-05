package com.solarclient.bedwars;

import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/**
 * <h1>Nick Hider / Streamer Mode</h1>
 *
 * <p>Troca o seu nome por um apelido em todo lugar que o client desenha: chat,
 * tab list, scoreboard e NameTags. Ideal para quem streama e nao quer que o
 * canal descubra a conta.</p>
 *
 * <p><b>Como funciona no chat:</b> o mixin entrega o componente original do
 * servidor para o {@link Events.ChatIncoming}. Este mod troca a string do nome
 * por um novo componente, marca o evento como cancelado e o mixin imprime a
 * versao trocada no lugar da original. O servidor nunca e alterado - e so o
 * que voce ve na sua tela.</p>
 */
public final class NickHider extends Mod {

    private final Minecraft mc = Minecraft.getMinecraft();
    private String alias = "Steve";

    public NickHider() {
        super("Nick Hider", "Esconde seu nome no chat, tab, placar e nametags",
                ModCategory.BEDWARS, 0);
        values.set("alias", "Steve");
    }

    public String getAlias() {
        return alias;
    }

    /** Aplica o apelido em um texto qualquer (tab, scoreboard, nametag). */
    public String apply(String text) {
        if (!isEnabled() || text == null) {
            return text;
        }
        String real = mc.getSession() == null ? null : mc.getSession().getUsername();
        if (real == null || real.isEmpty()) {
            return text;
        }
        return text.replace(real, alias);
    }

    @EventBus.Handler
    public void onChat(Events.ChatIncoming event) {
        if (!isEnabled()) {
            return;
        }
        syncSettings();
        String real = mc.getSession() == null ? null : mc.getSession().getUsername();
        if (real == null || !event.plain.contains(real)) {
            return;
        }
        // novo componente com o nome trocado + evento cancelado: o mixin usa o
        // replacement no lugar do original
        event.replacement = new ChatComponentText(event.plain.replace(real, alias));
        event.setCancelled(true);
    }

    @Override
    public void onTick() {
        syncSettings();
    }

    private void syncSettings() {
        alias = values.getString("alias", "Steve");
    }
}
