package com.solarclient.mods.render;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.util.ChatUtils;
import com.solarclient.util.Reflect;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;

import java.util.Map;

/**
 * BossBar.
 *
 * <p>Redesenha as barras de boss com o visual do client. A vanilla e
 * desenhada em {@code renderGameOverlay} tipo 2; o mixin cancela esse tipo
 * quando este mod esta ligado e o desenho acontece aqui.</p>
 *
 * <p>O mapa de bosses vive dentro do {@code BossHealthOverlay} (campo privado
 * do vanilla), por isso o acesso e feito por {@link Reflect} - assim o mod
 * nao quebra se o nome do campo/classe mudar no seu mapping.</p>
 */
public final class BossBar extends HudMod {

    private static BossBar instance;

    public BossBar() {
        super("BossBar", "Barra de boss com o tema do client",
                ModCategory.RENDER, 0, 0, Keyboard.KEY_K);
        instance = this;
        values.set("width", 182);
        values.set("showPercent", true);
    }

    public static BossBar get() {
        return instance;
    }

    /** Desenha as barras customizadas (chamado pelo mixin). */
    public static void renderReplacement(Events.Render2D event) {
        BossBar mod = instance;
        if (mod == null || !mod.isEnabled()) {
            return;
        }
        mod.draw(event);
    }

    private void draw(Events.Render2D event) {
        // GuiIngame -> campo com o Map<Integer, BossEvent> (privado no vanilla)
        GuiIngame ingame = Reflect.get(event.mc, GuiIngame.class);
        Object map = ingame == null ? null : Reflect.get(ingame, Map.class);
        if (!(map instanceof Map)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<?, ?> bosses = (Map<?, ?>) map;
        if (bosses.isEmpty()) {
            return;
        }

        int width = values.getInt("width", 182);
        int y = 8;
        SolarFont font = SolarFont.get("Sora", 13, false);

        for (Object boss : bosses.values()) {
            if (boss == null) {
                continue;
            }
            Boolean visible = (Boolean) Reflect.call(boss, Boolean.class, "isVisible", "shouldDisplay");
            if (visible != null && !visible) {
                continue;
            }

            String name = readName(boss);
            Float health = (Float) Reflect.call(boss, Float.class, "getHealth", "getHealthScale");
            Float max = (Float) Reflect.call(boss, Float.class, "getMaxHealth", "getMaxHealthScale");
            Integer color = (Integer) Reflect.call(boss, Integer.class, "getColor");
            if (health == null) {
                continue;
            }
            float percent = max == null || max <= 0f ? 0f : health / max;

            begin();
            RenderUtils.roundedRect(0, y, width, 10f, 4f, Colors.argb(190, 8, 10, 16));
            RenderUtils.roundedRect(0, y, Math.max(6f, width * percent), 10f, 4f,
                    Colors.withAlpha(color == null ? 0xB03AE0 : color, 0.9f));
            font.draw(name, 6, y + 8, Colors.WHITE);
            if (values.getBoolean("showPercent", true)) {
                font.drawRight(String.valueOf(Math.round(health)), width - 6, y + 8, Colors.WHITE);
            }
            setSize(width, 10);
            end();
            y += 12;
        }
    }

    private static String readName(Object boss) {
        Object component = Reflect.call(boss, IChatComponent.class, "getName", "getDisplayName");
        if (component instanceof IChatComponent) {
            return ChatUtils.getUnformattedText((IChatComponent) component);
        }
        Object text = Reflect.call(boss, String.class, "getName", "getDisplayName", "getUnformattedText");
        return text == null ? "Boss" : text.toString();
    }
}
