package com.solarclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderManager;

/**
 * Acesso ao {@code RenderManager} / {@code EntityRenderer}.
 *
 * <p>No 1.8.9 esses objetos ficam em campos privados e o nome varia com o
 * mapping. O {@link Reflect} resolve uma vez e guarda em cache, assim os mods
 * 3D (NameTags, cosmeticos, ESP de alerta) nao dependem do nome interno.</p>
 */
public final class RenderHelper {

    private static RenderManager cachedManager;

    private RenderHelper() {
    }

    public static RenderManager manager() {
        if (cachedManager != null) {
            return cachedManager;
        }
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager manager = Reflect.get(mc, RenderManager.class);
        if (manager == null) {
            EntityRenderer renderer = entityRenderer();
            if (renderer != null) {
                manager = Reflect.get(renderer, RenderManager.class);
            }
        }
        cachedManager = manager;
        return manager;
    }

    public static EntityRenderer entityRenderer() {
        return Reflect.get(Minecraft.getMinecraft(), EntityRenderer.class);
    }

    /** True quando o jogo esta com um mundo carregado (so entao desenhamos 3D). */
    public static boolean inWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.thePlayer != null && mc.theWorld != null;
    }
}
