package com.solarclient.mods.render;

import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.input.Keyboard;

/**
 * <h1>MOD DE EXEMPLO 3/3 - 1.7 Animations</h1>
 *
 * <p>Devolve ao Minecraft 1.8.9 as animacoes visuais do 1.7.10, que muita
 * gente prefere porque o "feel" do combate (blockhit curto, agachar sem o
 * corpo levantar) muda bastante a leitura da troca de golpes.</p>
 *
 * <p>Importante: <b>isto e 100% visual e local</b>. Nao mexe em pacote, nao
 * mexe em hitbox, nao mexe em movimento - e o que o modulo faz e escrever
 * valores nos bones do modelo via mixin. Nada disso existe no lado do
 * servidor, entao nao quebra anticheat.</p>
 *
 * <p>Sub-opcoes (todas independentes):</p>
 * <ul>
 *     <li><b>blockhit</b> - o braco vai para frente durante o swing (1.7) em
 *         vez do giro lateral do 1.8. Aplicado em {@code ModelPlayer};</li>
 *     <li><b>oldSneak</b> - ao agachar o corpo inteiro desliza para frente
 *         em vez de so rotacionar os bracos;</li>
 *     <li><b>oldRod</b> - a vara de pesca/pescoco na mao fica na posicao
 *         "segurando" do 1.7 em vez de apontando para frente;</li>
 *     <li><b>oldDamage</b> - inclinacao do corpo ao levar dano.</li>
 * </ul>
 *
 * <p>O mod e "burro" de proposito: ele guarda o estado e o mixin
 * {@code com.solarclient.mixin.SolarMixin$ModelPlayerMixin} pergunta
 * {@link #blockhit()} / {@link #oldSneak()} a cada frame. Assim da para
 * desligar tudo em tempo real sem recarregar o mixin.</p>
 */
public final class OneSevenAnimations extends Mod {

    private static OneSevenAnimations instance;

    public OneSevenAnimations() {
        super("1.7 Animations",
                "Blockhit, agachar e vara no estilo 1.7.10 (somente visual)",
                ModCategory.RENDER,
                Keyboard.KEY_B);

        instance = this;

        values.set("blockhit", true);
        values.set("oldSneak", true);
        values.set("oldRod", true);
        values.set("oldDamage", false);
    }

    public static OneSevenAnimations get() {
        return instance;
    }

    // ------------------------------------------------------------------ flags
    public boolean blockhit() {
        return isEnabled() && values.getBoolean("blockhit", true);
    }

    public boolean oldSneak() {
        return isEnabled() && values.getBoolean("oldSneak", true);
    }

    public boolean oldRod() {
        return isEnabled() && values.getBoolean("oldRod", true);
    }

    public boolean oldDamage() {
        return isEnabled() && values.getBoolean("oldDamage", false);
    }

    /**
     * Progresso do swing (0 = parado, 1 = fim do golpe).
     *
     * <p>No 1.8.9 {@code EntityLivingBase.swingItem} e um contador que sobe de
     * 0 ate 30 enquanto o braco balanca. Convertendo para 0..1 fica facil fazer
     * a curva do 1.7 (que e um "vaivem" e nao um giro).</p>
     */
    public static float swingProgress(EntityLivingBase entity) {
        if (entity == null || entity.swingItem <= 0) {
            return 0f;
        }
        return Math.min(1f, entity.swingItem / 30f);
    }

    /**
     * Curva do blockhit 1.7: o braco sobe rapido e volta rapido (ease in-out),
     * com o maximo em 45 graus - bem diferente do giro do 1.8.
     *
     * @return rotacao em radianos, ou 0 se o mod/feature estiver desligado
     */
    public static float blockhitAngle(EntityLivingBase entity) {
        OneSevenAnimations mod = get();
        if (mod == null || !mod.blockhit()) {
            return 0f;
        }
        float progress = swingProgress(entity);
        if (progress <= 0f) {
            return 0f;
        }
        // 0 -> 0.78rad -> 0 (vaivem suave)
        return (float) Math.sin(progress * Math.PI) * 0.78f;
    }
}
