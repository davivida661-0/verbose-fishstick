package com.solarclient.mods.pvp;

import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tab List.
 *
 * <p>Redesenha a lista de jogadores do topo da tela (o "Tab" do vanilla) com o
 * visual do client. Como a vanilla e desenhada pelo jogo, este overlay e
 * aplicado <b>depois</b> dela (o mixin chama
 * {@link #renderOverlay(Minecraft, int, int, float)} no fim do render do HUD),
 * cobrindo a original com o fundo opaco.</p>
 */
public final class TabList extends HudMod {

    private static final int ROW = 10;

    private static TabList instance;

    public TabList() {
        super("Tab List", "Lista de jogadores com ping e cabecalho",
                ModCategory.RENDER, 0, 0, Keyboard.KEY_TAB);
        values.set("showPing", true);
        values.set("showHeader", true);
        values.set("sortByPing", false);
        instance = this;
    }

    /** Chamado pelo mixin depois que o vanilla desenhou a tab original. */
    public static void renderOverlay(Minecraft mc, int screenWidth, int screenHeight, float partial) {
        TabList mod = instance;
        if (mod == null || !mod.isEnabled() || mc.thePlayer == null) {
            return;
        }
        // so desenha quando a tab vanilla esta visivel (tecla Tab segurada)
        if (!mc.gameSettings.keyBindPlayerList.isKeyDown()) {
            return;
        }
        mod.draw(mc, screenWidth);
    }

    private void draw(Minecraft mc, int screenWidth) {
        Collection<NetworkPlayerInfo> infos = mc.getNetHandler() == null
                ? null : mc.getNetHandler().getPlayerInfo();
        if (infos == null || infos.isEmpty()) {
            return;
        }

        List<NetworkPlayerInfo> list = new ArrayList<>(infos);
        final boolean byPing = values.getBoolean("sortByPing", false);
        Collections.sort(list, new Comparator<NetworkPlayerInfo>() {
            @Override
            public int compare(NetworkPlayerInfo a, NetworkPlayerInfo b) {
                if (byPing) {
                    return Integer.compare(a.getLatency(), b.getLatency());
                }
                return a.getGameProfile().getName().compareToIgnoreCase(b.getGameProfile().getName());
            }
        });

        SolarFont font = SolarFont.get("Sora", 13, false);
        int width = 200;
        int headerHeight = values.getBoolean("showHeader", true) ? 16 : 4;
        int height = headerHeight + list.size() * ROW + 6;

        int x = (screenWidth - width) / 2;
        int y = 4;

        begin();
        RenderUtils.shadow(x, y, width, height, 8f, Colors.argb(150, 0, 0, 0), 3);
        RenderUtils.roundedRect(x, y, width, height, 8f, Colors.argb(165, 10, 12, 20));
        RenderUtils.roundedOutline(x, y, width, height, 8f, Colors.argb(70, 255, 255, 255));

        if (values.getBoolean("showHeader", true)) {
            font.drawCentered("Jogadores online - " + list.size(), x + width / 2f, y + 12,
                    Theme.TEXT_SECONDARY);
        }

        int lineY = y + headerHeight + 10;
        for (NetworkPlayerInfo info : list) {
            boolean self = info.getGameProfile().getName()
                    .equals(mc.getSession().getUsername());
            font.draw(info.getGameProfile().getName(), x + 8, lineY,
                    self ? Theme.getAccent() : Theme.TEXT_PRIMARY);

            if (values.getBoolean("showPing", true)) {
                int ping = info.getLatency();
                int color = ping < 90 ? Colors.rgb(0x3E, 0xE0, 0xA4)
                        : ping < 180 ? Theme.WARNING : Theme.DANGER;
                float ratio = Math.min(1f, ping / 300f);
                RenderUtils.roundedRect(x + width - 46, lineY - 4f, 38, 3f, 1.5f,
                        Colors.argb(80, 255, 255, 255));
                RenderUtils.roundedRect(x + width - 46, lineY - 4f, Math.max(3f, 38 * ratio), 3f, 1.5f,
                        color);
            }
            lineY += ROW;
        }

        setSize(width, height);
        end();
    }
}
