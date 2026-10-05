package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import com.solarclient.util.ChatUtils;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

/**
 * Item Info.
 *
 * <p>Mostra o item que esta na mao: nome, quantidade e durabilidade - com
 * icone de alerta quando o durability esta baixo. Ajuda a trocar de sword no
 * momento certo sem abrir o inventario.</p>
 */
public final class ItemInfo extends HudMod {

    public ItemInfo() {
        super("Item Info", "Nome, quantidade e durabilidade do item na mao",
                ModCategory.COMBAT, 4, 190, Keyboard.KEY_G);
        values.set("showCount", true);
        values.set("warnPercent", 25);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (event.mc.thePlayer == null) {
            return;
        }
        ItemStack stack = event.mc.thePlayer.getHeldItem();
        if (stack == null || stack.isEmpty()) {
            return;
        }

        SolarFont font = SolarFont.get();
        // displayName vem do protocolo ja com cor; o HUD nao usa cor no texto
        String name = ChatUtils.getUnformattedText(stack.getDisplayName());

        int w = Math.max(110, (int) font.width(name) + 20);
        int h = 34;

        begin();
        drawPanel(w, h, 0.5f, 6f);

        font.draw(name, 8, 15, Theme.TEXT_PRIMARY);
        if (values.getBoolean("showCount", true) && stack.stackSize > 1) {
            font.drawRight("x" + stack.stackSize, w - 8, 15, Theme.TEXT_SECONDARY);
        }

        if (stack.isItemStackDamageable()) {
            int max = stack.getMaxDamage();
            int damage = stack.getItemDamage();
            float percent = max <= 0 ? 0f : 1f - damage / (float) max;
            int warn = values.getInt("warnPercent", 25);
            int color = percent * 100 <= warn ? Theme.DANGER
                    : percent <= 0.5f ? Theme.WARNING : Theme.SUCCESS;

            RenderUtils.roundedRect(8, 20, w - 16, 4f, 2f, Colors.argb(80, 255, 255, 255));
            RenderUtils.roundedRect(8, 20, Math.max(4f, (w - 16) * percent), 4f, 2f, color);
            font.drawRight(String.valueOf(max - damage), w - 8, 32, Theme.TEXT_SECONDARY);
        }

        setSize(w, h);
        end();
    }
}
