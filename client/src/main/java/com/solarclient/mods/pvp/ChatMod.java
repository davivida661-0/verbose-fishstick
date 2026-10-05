package com.solarclient.mods.pvp;

import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.Calendar;

/**
 * Chat Mod.
 *
 * <p>Troca o chat de 1.8 (fundo cinza, 10 linhas, some rapido) por um chat no
 * estilo dos clients grandes: fundo escuro translucido, cantos arredondados,
 * timestamp e altura maior.</p>
 *
 * <p>O mod <b>escuta o chat sozinho</b>: como todo mod e registrado no
 * {@code EventBus} no {@code SolarClient#init()}, basta um metodo anotado com
 * {@link EventBus.Handler} para receber as mensagens. O desenho acontece por
 * cima do chat que o jogo ja desenhou.</p>
 */
public final class ChatMod extends HudMod {

    private static final int MAX_LINES = 10;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final String[] lines = new String[MAX_LINES];
    private int count;

    public ChatMod() {
        super("Chat", "Chat moderno com timestamp e fundo arredondado",
                ModCategory.RENDER, 4, 310, Keyboard.KEY_T);
        values.set("timestamps", true);
        values.set("background", true);
        values.set("fontSize", 14);
    }

    /** Handler: toda mensagem que chega do servidor entra no buffer. */
    @EventBus.Handler
    public void onChat(Events.ChatIncoming event) {
        if (!isEnabled() || event.plain == null || event.plain.isEmpty()) {
            return;
        }
        for (int i = Math.min(count, MAX_LINES - 1); i > 0; i--) {
            lines[i] = lines[i - 1];
        }
        lines[0] = event.plain;
        count = Math.min(count + 1, MAX_LINES);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (count == 0 || !values.getBoolean("background", true)) {
            return;
        }
        SolarFont font = SolarFont.get("Sora", values.getInt("fontSize", 14), false);
        int lineHeight = 11;
        int boxHeight = 10 + lineHeight * count;
        int boxWidth = 220;

        // Recorta para nao invadir a hotbar
        RenderUtils.scissor(0, event.screenHeight - boxHeight - 16, boxWidth + 8, boxHeight + 8);
        RenderUtils.begin2D();
        RenderUtils.roundedRect(2, event.screenHeight - boxHeight - 12, boxWidth, boxHeight,
                6f, Colors.argb(120, 8, 10, 16));

        String stamp = timestamp();
        for (int i = 0; i < count; i++) {
            float y = event.screenHeight - boxHeight - 2 + i * lineHeight;
            if (values.getBoolean("timestamps", true)) {
                String prefix = "[" + stamp + "] ";
                font.draw(prefix, 8, y + 9, Colors.argb(120, 255, 255, 255));
                font.draw(lines[i], 8 + font.width(prefix), y + 9, Theme.TEXT_PRIMARY);
            } else {
                font.draw(lines[i], 8, y + 9, Theme.TEXT_PRIMARY);
            }
        }
        RenderUtils.end2D();
        RenderUtils.endScissor();

        setSize(boxWidth, boxHeight);
    }

    private static String timestamp() {
        Calendar now = Calendar.getInstance();
        return String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
    }
}
