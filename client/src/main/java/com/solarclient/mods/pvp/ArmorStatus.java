package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.util.ChatUtils;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

/**
 * Armor Status.
 *
 * <p>Mostra as 4 pecas de armadura (bota, perna, torso, elmo) com a barra de
 * durabilidade. A cor da barra vai de verde (novo) a vermelho (quebrando
 * qualquer hora), que e a unica coisa que importa em um briga-longa: saber se
 * a armadura vai estourar antes do inimigo te matar.</p>
 */
public final class ArmorStatus extends HudMod {

    private static final int ICON = 16;
    private static final int GAP = 2;

    public ArmorStatus() {
        super("Armor Status", "Durabilidade da armadura com aviso de quebra",
                ModCategory.COMBAT, 4, 130, Keyboard.KEY_M);
        values.set("warnPercent", 20);
        values.set("showHotbarArmor", true);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (event.mc.thePlayer == null) {
            return;
        }
        int warn = Math.max(0, Math.min(100, values.getInt("warnPercent", 20)));

        int width = ICON * 4 + GAP * 3;
        begin();
        if (values.getBoolean("showHotbarArmor", true)) {
            drawPanel(width + 8, ICON + 8, 0.45f, 5f);
        }

        // a ordem do inventario do Minecraft: 3 = pe, 2 = perna, 1 = torso, 0 = elmo
        ItemStack[] armor = event.mc.thePlayer.inventory.armorInventory;
        for (int i = 0; i < 4; i++) {
            float x = i * (ICON + GAP);
            ItemStack stack = armor[i];

            // caixa do slot
            RenderUtils.roundedRect(x, 0, ICON, ICON, 4f, Colors.argb(120, 30, 34, 48));
            RenderUtils.roundedOutline(x, 0, ICON, ICON, 4f, Colors.argb(60, 255, 255, 255));

            if (stack == null || stack.isEmpty()) {
                continue;
            }

            // barra de durabilidade na parte de baixo do slot
            if (stack.isItemStackDamageable()) {
                int max = stack.getMaxDamage();
                int damage = stack.getItemDamage();
                float percent = max <= 0 ? 0f : 1f - (damage / (float) max);
                int color = percent <= warn / 100f ? Colors.rgb(0xFF, 0x5C, 0x5C)
                        : percent <= 0.5f ? Colors.rgb(0xFF, 0xC8, 0x57)
                        : Colors.rgb(0x3E, 0xE0, 0xA4);
                RenderUtils.roundedRect(x + 2, ICON - 4f, ICON - 4, 2f, 1f,
                        Colors.argb(90, 0, 0, 0));
                RenderUtils.roundedRect(x + 2, ICON - 4f, (ICON - 4) * percent, 2f, 1f, color);
            }

            // nome abreviado do item (ex.: "DIAMOND") em cima da barra
            String label = ChatUtils.shortenItem(stack);
            if (!label.isEmpty()) {
                com.solarclient.ui.SolarFont.get().drawCentered(label, x + ICON / 2f, 9f,
                        Colors.argb(200, 235, 240, 250));
            }
        }

        setSize(width + 8, ICON + 8);
        end();
    }
}
