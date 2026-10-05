package com.solarclient.ui;

/**
 * Utilitarios de cor. Tudo no client e ARGB empacotado em um {@code int}
 * (0xAARRGGBB) e convertido para {@code glColor4f} na hora de desenhar.
 */
public final class Colors {

    public static final int TRANSPARENT = 0x00000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int GRAY = 0xFF8A8F9A;

    private Colors() {
    }

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int rgb(int r, int g, int b) {
        return argb(255, r, g, b);
    }

    /** Troca o alpha (0-255) mantendo o RGB. */
    public static int withAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
    }

    /** Troca o alpha usando 0.0 - 1.0. */
    public static int withAlpha(int color, float alpha) {
        return withAlpha(color, (int) (clamp(alpha) * 255f));
    }

    public static int getAlpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    /** Interpola duas cores (t = 0 devolve {@code a}, t = 1 devolve {@code b}). */
    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return argb(
                (int) (getAlpha(a) + (getAlpha(b) - getAlpha(a)) * t),
                (int) (getRed(a) + (getRed(b) - getRed(a)) * t),
                (int) (getGreen(a) + (getGreen(b) - getGreen(a)) * t),
                (int) (getBlue(a) + (getBlue(b) - getBlue(a)) * t));
    }

    public static int darker(int color, float factor) {
        return argb(getAlpha(color),
                (int) (getRed(color) * factor),
                (int) (getGreen(color) * factor),
                (int) (getBlue(color) * factor));
    }

    public static int lighter(int color, float factor) {
        return argb(getAlpha(color),
                (int) (getRed(color) * (1f + factor)),
                (int) (getGreen(color) * (1f + factor)),
                (int) (getBlue(color) * (1f + factor)));
    }

    /** Gradiente HSL usado no raridade dos cosmeticos e no fundo do menu. */
    public static int rainbow(float time, float speed) {
        float h = (time * speed) % 1f;
        if (h < 0) {
            h += 1f;
        }
        return hsl(h, 0.85f, 0.60f);
    }

    /** Converte HSL (h em voltas, s/l em 0-1) para ARGB. */
    public static int hsl(float h, float s, float l) {
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float hp = h * 6f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float r;
        float g;
        float b;
        if (hp < 1f) {
            r = c; g = x; b = 0;
        } else if (hp < 2f) {
            r = x; g = c; b = 0;
        } else if (hp < 3f) {
            r = 0; g = c; b = x;
        } else if (hp < 4f) {
            r = 0; g = x; b = c;
        } else if (hp < 5f) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        float m = l - c / 2f;
        return rgb((int) ((r + m) * 255), (int) ((g + m) * 255), (int) ((b + m) * 255));
    }

    public static String hex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
