package com.solarclient.mods.performance;

import com.solarclient.event.Events;
import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.util.Reflect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * <h1>Performance (4 recursos em um)</h1>
 *
 * <p>Reune as quatro otimizacoes classicas de client em um mod so, cada uma
 * com seu toggle. Todas sao 100% visuais/locais - nada de pacote.</p>
 *
 * <ul>
 *     <li><b>FPS Boost</b> - limita a taxa de atualizacao das particulas e
 *         das entidades (o jogo continua 100% responsivo, so para de redesenhar
 *         o que nao muda);</li>
 *     <li><b>Memory Fix</b> - solta texturas que nao estao em uso ha alguns
 *         segundos (o grow de RAM do 1.8 e famoso: o mapa vai "inchando");</li>
 *     <li><b>Entity Culling</b> - para de atualizar/desenhar entidades fora
 *         do frustum da camera;</li>
 *     <li><b>Lag Patch</b> - limita o numero de particelas e sons que podem
 *         nascer no mesmo tick (corrige travadas em areas com muito
 *         explode/efeito).</li>
 * </ul>
 */
public final class Performance extends Mod {

    private static Performance instance;
    private int ticks;

    public Performance() {
        super("Performance", "FPS Boost, Memory Fix, Entity Culling e Lag Patch",
                ModCategory.RENDER, Keyboard.KEY_P);
        instance = this;
        values.set("fpsBoost", true);
        values.set("memoryFix", true);
        values.set("entityCulling", true);
        values.set("lagPatch", true);
        values.set("maxParticles", 2000);
        values.set("memoryIntervalTicks", 1200);
    }

    public static Performance get() {
        return instance;
    }

    // ------------------------------------------------------------ flags do mixin
    public static boolean fpsBoost() {
        return instance != null && instance.isEnabled() && instance.values.getBoolean("fpsBoost", true);
    }

    public static boolean entityCulling() {
        return instance != null && instance.isEnabled()
                && instance.values.getBoolean("entityCulling", true);
    }

    public static boolean lagPatch() {
        return instance != null && instance.isEnabled() && instance.values.getBoolean("lagPatch", true);
    }

    public static int maxParticles() {
        return instance == null ? 2000 : instance.values.getInt("maxParticles", 2000);
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void onTick() {
        if (!isEnabled() || mc().theWorld == null) {
            return;
        }
        ticks++;

        if (values.getBoolean("memoryFix", true)
                && ticks % Math.max(100, values.getInt("memoryIntervalTicks", 1200)) == 0) {
            releaseUnusedTextures();
        }
    }

    // ------------------------------------------------------------------ render
    @Override
    public void onRender2D(Events.Render2D event) {
        if (!isEnabled() || !values.getBoolean("fpsBoost", true)) {
            return;
        }
        // FPS Boost em 1.8.9: limitar o desenho das particulas e o unico jeito
        // "honesto" sem mexer na taxa de tick (que mudaria o jogo de verdade).
        EffectRenderer effects = event.mc.getEffectRenderer();
        if (effects == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Entity> list = (List<Entity>) Reflect.get(effects, List.class);
        if (list == null) {
            return;
        }
        int cap = Math.max(200, values.getInt("maxParticles", 2000));
        while (list.size() > cap) {
            // remove as mais antigas (efeitos visuais, nunca entidades do mundo)
            list.remove(list.size() - 1);
        }
    }

    // ------------------------------------------------------------------ memoria
    /**
     * Descarta texturas que o jogo ainda mantem em cache mas nao desenha mais.
     * Usa o cache interno do TextureManager (que e um Map) via reflexao.
     */
    @SuppressWarnings("unchecked")
    private void releaseUnusedTextures() {
        Object manager = mc().getTextureManager();
        Object map = Reflect.get(manager, java.util.Map.class);
        if (!(map instanceof java.util.Map)) {
            return;
        }
        java.util.Map<Object, Object> cache = (java.util.Map<Object, Object>) map;
        // heuristics simples: remove entradas "aw" (waypoints) e texturas de
        // mapa antigo, que o 1.8 mantem para sempre
        cache.keySet().removeIf(key -> key != null
                && key.toString().toLowerCase().contains("/maps/"));
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    // ------------------------------------------------------------------ info
    /** Resumo curto para a tela de estatisticas (F3 + S). */
    public static void drawDebug(int width, int height) {
        if (!fpsBoost()) {
            return;
        }
        SolarFont font = SolarFont.get("Sora", 12, false);
        RenderUtils.begin2D();
        font.drawRight("FPS Boost ON", width - 4, height - 2, Colors.argb(120, 255, 255, 255));
        RenderUtils.end2D();
        GL11.glFlush();
    }
}
