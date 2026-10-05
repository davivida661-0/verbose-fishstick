package com.solarclient.mods.render;

import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import org.lwjgl.input.Keyboard;

/**
 * FullBright.
 *
 * <p>Remove a nevoa (fog) do mundo, deixando o mapa totalmente iluminado. O
 * mod nao desenha nada sozinho: o mixin do {@code EntityRenderer} chama
 * {@link #isActive()} no inicio de {@code setupFog} e empurra a nevoa para
 * 1000 blocos (ou desliga de vez).</p>
 *
 * <p>E 100% client-side: o servidor nunca recebe nada disso.</p>
 */
public final class FullBright extends Mod {

    private static FullBright instance;

    public FullBright() {
        super("FullBright", "Tira a nevoa e deixa o mapa iluminado",
                ModCategory.RENDER, Keyboard.KEY_H);
        instance = this;
        values.set("mode", 0);   // 0 = linear 1000 | 1 = sem fog | 2 = gamma maximo
    }

    public static FullBright get() {
        return instance;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    public int getMode() {
        return values.getInt("mode", 0);
    }
}
