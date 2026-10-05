package com.solarclient.mods.render;

import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import com.solarclient.util.KeyBindingHelper;
import net.minecraft.client.Minecraft;

/**
 * ToggleSneak.
 *
 * <p>Um toque em agachar liga/desliga o agachamento, sem precisar segurar a
 * tecla. O truque e chamar {@code KeyBinding.setKeyBindState}: o jogo passa a
 * acreditar que a tecla esta segurada, entao o jogador fica agachado de
 * verdade (hitbox menor, sem cair da borda) e o servidor recebe o estado
 * normal de sneak.</p>
 */
public final class ToggleSneak extends Mod {

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasDown;
    private boolean toggled;

    public ToggleSneak() {
        super("ToggleSneak", "Agachar com um toque, sem segurar a tecla",
                ModCategory.RENDER, 0);
    }

    @Override
    public void onTick() {
        int keyCode = mc.gameSettings.keyBindSneak.keyCode;
        boolean down = mc.gameSettings.keyBindSneak.isKeyDown();

        // borda de subida: exatamente um toggle por toque
        if (down && !wasDown) {
            toggled = !toggled;
        }
        wasDown = down;

        if (toggled) {
            KeyBindingHelper.press(keyCode, true);
        } else if (!down) {
            KeyBindingHelper.press(keyCode, false);
        }
    }

    @Override
    protected void onDisable() {
        toggled = false;
        KeyBindingHelper.press(mc.gameSettings.keyBindSneak.keyCode, false);
    }
}
