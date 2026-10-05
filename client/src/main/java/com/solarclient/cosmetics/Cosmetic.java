package com.solarclient.cosmetics;

import net.minecraft.entity.player.EntityPlayer;

/**
 * <h1>Cosmetico (base)</h1>
 *
 * <p>Um cosmetico sabe se desenhar em um jogador. O {@link CosmeticManager}
 * chama {@link #render(EntityPlayer, float)} uma vez por jogador por frame,
 * e o proprio cosmetico cuida de push/pop de matriz, textura e blend.</p>
 *
 * <p>Para criar um novo: estenda esta classe, implemente
 * {@link #render(EntityPlayer, float)} e registre o tipo em
 * {@link CosmeticType}. Nada mais precisa ser alterado.</p>
 */
public abstract class Cosmetic {

    private final String id;
    private final CosmeticType type;
    private final Rarity rarity;
    private final String displayName;

    protected Cosmetic(String id, CosmeticType type, Rarity rarity, String displayName) {
        this.id = id;
        this.type = type;
        this.rarity = rarity;
        this.displayName = displayName;
    }

    public final String getId() {
        return id;
    }

    public final CosmeticType getType() {
        return type;
    }

    public final Rarity getRarity() {
        return rarity;
    }

    public final String getDisplayName() {
        return displayName;
    }

    /**
     * Desenha o cosmetico <b>no jogador</b>.
     *
     * @param player       jogador dono do cosmetico
     * @param partialTicks interpolacao (0..1) para a animacao ficar suave
     */
    public abstract void render(EntityPlayer player, float partialTicks);
}
