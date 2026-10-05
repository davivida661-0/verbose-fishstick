package com.solarclient.mods.render;

import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;

/**
 * No Hurt Cam.
 *
 * <p>A camera treme quando o jogador leva dano. Este mod zera esse tremor -
 * o mixin do {@code EntityRenderer} chama {@link #isActive()} e devolve 0.0
 * para {@code hurtCameraEffect}.</p>
 */
public final class NoHurtCam extends Mod {

    private static NoHurtCam instance;

    public NoHurtCam() {
        super("No Hurt Cam", "Tira o tremor de camera ao levar dano",
                ModCategory.RENDER, 0);
        instance = this;
    }

    public static NoHurtCam get() {
        return instance;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }
}
