package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Potion Status.
 *
 * <p>Lista os efeitos ativos com o tempo restante em segundos. No 1.8 o
 * inventario ja mostra isso, mas fica escondido no meio da tela de inventario
 * aberto durante o combate - por isso o mod.</p>
 */
public final class PotionStatus extends HudMod {

    private static final int ROW = 14;

    public PotionStatus() {
        super("Potion Status", "Efeitos ativos e tempo restante",
                ModCategory.COMBAT, 4, 160, Keyboard.KEY_H);
        values.set("showTimer", true);
        values.set("maxRows", 5);
        values.set("compact", false);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (event.mc.thePlayer == null) {
            return;
        }
        Collection<PotionEffect> effects = event.mc.thePlayer.getActivePotionEffects();
        if (effects == null || effects.isEmpty()) {
            return;
        }

        List<PotionEffect> list = new ArrayList<>(effects);
        int maxRows = Math.max(1, values.getInt("maxRows", 5));
        if (list.size() > maxRows) {
            list = list.subList(0, maxRows);
        }

        int fontSize = values.getBoolean("compact", false) ? 12 : 16;
        SolarFont font = SolarFont.get("Sora", fontSize, false);
        int boxWidth = 96;
        int boxHeight = list.size() * ROW + 6;

        begin();
        drawPanel(boxWidth, boxHeight, 0.45f, 6f);

        int y = 12;
        for (PotionEffect effect : list) {
            // bolinha colorida com a cor do efeito
            RenderUtils.circle(9, y - 4f, 3.5f, colorOf(effect));
            font.draw(nameOf(effect.getPotion()), 18, y, Theme.TEXT_PRIMARY);

            if (values.getBoolean("showTimer", true)) {
                // 1.8.9: getDuration() devolve ticks (20 ticks = 1s)
                int seconds = effect.getDuration() / 20;
                font.drawRight(format(seconds), 90, y, Theme.TEXT_SECONDARY);
            }
            y += ROW;
        }

        setSize(boxWidth, boxHeight);
        end();
    }

    private static String format(int seconds) {
        if (seconds >= 60) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }

    private static int colorOf(PotionEffect effect) {
        String id = effect.getPotion().getName();
        if ("moveSpeed".equals(id)) {
            return Colors.rgb(0x5B, 0x9B, 0xD5);
        }
        if ("damageBoost".equals(id)) {
            return Colors.rgb(0xE0, 0x5B, 0x5B);
        }
        if ("heal".equals(id)) {
            return Colors.rgb(0xE0, 0x7B, 0x9B);
        }
        if ("fireResistance".equals(id)) {
            return Colors.rgb(0xE0, 0xA0, 0x3B);
        }
        if ("jumpBoost".equals(id)) {
            return Colors.rgb(0x8A, 0xE0, 0x6B);
        }
        if ("invisibility".equals(id)) {
            return Colors.rgb(0xB0, 0xB8, 0xC8);
        }
        return Colors.rgb(0xB0, 0x6A, 0xE0);
    }

    /** Traduz o nome interno do efeito para algo legivel. */
    private static String nameOf(Potion potion) {
        String id = potion.getName();
        if (id == null || id.isEmpty()) {
            return "Efeito";
        }
        switch (id) {
            case "moveSpeed":
                return "Velocidade";
            case "moveSlowdown":
                return "Lentidao";
            case "digSpeed":
                return "Mineracao";
            case "damageBoost":
                return "Forca";
            case "heal":
                return "Regeneracao";
            case "fireResistance":
                return "Resist. Fogo";
            case "jumpBoost":
                return "Salto";
            case "confusion":
                return "Confusao";
            case "blindness":
                return "Cegueira";
            case "invisibility":
                return "Invisibilidade";
            case "resistance":
                return "Resistencia";
            case "waterBreathing":
                return "Respiracao";
            default:
                return id;
        }
    }
}
