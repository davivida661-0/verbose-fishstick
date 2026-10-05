package com.solarclient.mods.render;

import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import org.lwjgl.input.Keyboard;

/**
 * Clear Glass.
 *
 * <p>Desenha o vidro (vidro,etereo, glowstone, barreiras de agua estagada)
 * na camada "cutout" em vez da camada translucida. Resultado: da para ver
 * atraves sem o visual azulado e sem o atraso de ordenacao.</p>
 *
 * <p>O mod so expoe o estado; quem decide a camada de render e o mixin em
 * {@code BlockRenderLayer#canRender}.</p>
 */
public final class ClearGlass extends Mod {

    private static ClearGlass instance;

    public ClearGlass() {
        super("Clear Glass", "Vidro e blocos solidos sem a camada translucida",
                ModCategory.RENDER, Keyboard.KEY_V);
        instance = this;
        values.set("blocks", 0);   // bitmask: 1 vidro | 2 etereo | 4 barreiras de agua
    }

    public static ClearGlass get() {
        return instance;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    public int getBlockMask() {
        return values.getInt("blocks", 0);
    }
}
