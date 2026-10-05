package com.solarclient.bedwars;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.util.Reflect;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

/**
 * <h1>Fireball Jump (timer)</h1>
 *
 * <p>Quando voce leva dano de uma fireball, existe uma janela curta em que
 * pular cancela o knockback. Este mod so <b>mostra o tempo</b> dessa janela
 * (uma barra) - a decision continua sendo sua.</p>
 *
 * <p>Para saber que o dano veio de uma fireball, o mod le o campo privado
 * {@code lastDamageSource} do {@code EntityLivingBase} (que no 1.8.9 guarda a
 * ultima fonte de dano) atraves do {@link Reflect}. Se o seu mapping nao
 * tiver esse campo, o mod simplesmente nunca ativa.</p>
 */
public final class FireballJump extends HudMod {

    private final Minecraft mc = Minecraft.getMinecraft();
    private int lastHurtTime;
    private long windowStart;

    public FireballJump() {
        super("Fireball Jump", "Timer da janela de pulo apos levar fireball",
                ModCategory.BEDWARS, 4, 150, 0);
        values.set("windowMs", 1500);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (windowStart == 0 || mc.thePlayer == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - windowStart;
        int window = values.getInt("windowMs", 1500);
        if (elapsed > window) {
            windowStart = 0;
            return;
        }

        float percent = 1f - elapsed / (float) window;
        int w = 150;
        begin();
        drawPanel(w, 28, 0.5f, 7f);
        SolarFont font = SolarFont.get("Sora", 13, false);
        font.draw("PULO (fireball)", 8, 16, Colors.WHITE);
        RenderUtils.roundedRect(8, 20, w - 16, 4f, 2f, Colors.argb(80, 255, 255, 255));
        RenderUtils.roundedRect(8, 20, Math.max(4f, (w - 16) * percent), 4f, 2f,
                Colors.rgb(0xFF, 0xC8, 0x57));
        setSize(w, 28);
        end();
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null) {
            return;
        }
        EntityLivingBase player = mc.thePlayer;
        int hurt = player.hurtTime;

        // borda de subida do hurtTime = tomou dano neste tick
        if (hurt > lastHurtTime && hurt > 0) {
            Entity source = Reflect.get(player, Entity.class);
            if (source != null && isFireball(source)) {
                windowStart = System.currentTimeMillis();
            }
        }
        lastHurtTime = hurt;
    }

    private static boolean isFireball(Entity source) {
        String name = source.getClass().getSimpleName().toLowerCase();
        return name.contains("fireball");
    }
}
