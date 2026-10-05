package com.solarclient.ui;

/**
 * Curvas de easing usadas nas animacoes do ClickGUI e do HUD Editor.
 * Todas recebem e devolvem valores de 0.0 a 1.0.
 */
public final class Easing {

    private Easing() {
    }

    public static float linear(float t) {
        return t;
    }

    /** Aceleracao suave, a mais usada em UI. */
    public static float outCubic(float t) {
        float f = t - 1f;
        return f * f * f + 1f;
    }

    public static float inOutCubic(float t) {
        return t < 0.5f
                ? 4f * t * t * t
                : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    /** Passa um pouco do alvo e volta: bom para "pop" de botao. */
    public static float outBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float f = t - 1f;
        return 1f + c3 * f * f * f + c1 * f * f;
    }

    public static float outExpo(float t) {
        return t >= 1f ? 1f : 1f - (float) Math.pow(2f, -10f * t);
    }

    /** Suave nas pontas, util para barras que vao encolher. */
    public static float inOutQuad(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }
}
