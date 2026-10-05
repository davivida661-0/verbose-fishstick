package com.solarclient.mods.render;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import org.lwjgl.input.Keyboard;

/**
 * Hit Color.
 *
 * <p>Pinta a tela por cima quando o jogador leva dano (a "hit vignette" do
 * client). O padrao e vermelho no dano e verde quando voce acerta alguem.</p>
 *
 * <p>Como detectar: {@code hurtTime} do jogador comeca em 10 e desce a cada
 * tick. O "acabou de levar dano" e quando ele sobe; o "acertou alguem" e
 * quando o {@code hurtTime} de um inimigo perto comeca a descer.</p>
 */
public final class HitColor extends HudMod {

    private int lastHurtTime;
    private float flash;
    private boolean tookDamage;

    public HitColor() {
        super("Hit Color", "Pinta a tela ao levar/sacar dano",
                ModCategory.RENDER, 0, 0, Keyboard.KEY_U);
        values.set("damageColor", 0xE02020);
        values.set("alpha", 0.35f);
        values.set("fade", 0.85f);
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (event.mc.thePlayer == null) {
            return;
        }
        int hurt = event.mc.thePlayer.hurtTime;

        // borda de subida = tomou dano agora
        if (hurt > lastHurtTime) {
            flash = 1f;
            tookDamage = true;
        } else if (hurt < lastHurtTime && hurt > 0) {
            // hurtTime descendo = um golpe foi acertado
            flash = 1f;
            tookDamage = false;
        }
        lastHurtTime = hurt;

        flash *= (float) values.getDouble("fade", 0.85d);
        if (flash < 0.01f) {
            return;
        }

        int color = tookDamage
                ? values.getColor("damageColor", 0xE02020)
                : Colors.rgb(0x20, 0xE0, 0x60);

        // gradiente escuro nas bordas (vinheta) - bem mais suave que um retangulo
        begin();
        float alpha = (float) values.getDouble("alpha", 0.35d) * flash;
        RenderUtils.gradient(0, 0, event.screenWidth, event.screenHeight,
                Colors.withAlpha(color, alpha * 0.9f), Colors.withAlpha(color, 0f), true);
        end();
        setSize(event.screenWidth, event.screenHeight);
    }
}
