package com.solarclient.cosmetics;

import com.solarclient.ui.Colors;

/**
 * Raridade do cosmetico.
 *
 * <p><b>Sobre os "shaders":</b> o 1.8.9 nao tem pipeline de shader, entao
 * o efeito de raridade aqui e feito com blend aditivo + uma passada de brilho
 * pulsante (o mesmo visual que o Badlion usa para o contorno). Se voce quiser
 * shader de verdade, o ponto de extensao e
 * {@link AnimatedCape#applyRarityFx(Rarity, float)}.</p>
 */
public enum Rarity {

    COMMON(0xFFB0B6C3, "Comum", 0.0f),
    RARE(0xFF4FA9FF, "Raro", 0.25f),
    EPIC(0xFFB15CFF, "Epico", 0.5f),
    LEGENDARY(0xFFFFC24A, "Lendario", 0.75f);

    private final int color;
    private final String displayName;
    /** Intensidade do brilho (0 = nenhum). */
    private final float glow;

    Rarity(int color, String displayName, float glow) {
        this.color = color;
        this.displayName = displayName;
        this.glow = glow;
    }

    public int getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getGlow() {
        return glow;
    }

    public static Rarity fromName(String name) {
        if (name == null) {
            return COMMON;
        }
        for (Rarity rarity : values()) {
            if (rarity.name().equalsIgnoreCase(name)) {
                return rarity;
            }
        }
        return COMMON;
    }

    /** Cor usada no texto da GUI. */
    public int uiColor() {
        return Colors.withAlpha(color, 0xFF);
    }
}
