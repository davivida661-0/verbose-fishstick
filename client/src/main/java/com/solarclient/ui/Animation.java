package com.solarclient.ui;

/**
 * Animacao interpolada simples (usada no ClickGUI, HUD Editor e GUI de cosmeticos).
 *
 * <p>Estrutura: um valor atual que persegue um valor alvo com
 * {@link Easing#outCubic}. Chame {@link #update()} uma vez por tick.</p>
 *
 * <pre>{@code
 * private final Animation open = new Animation(0f);
 * open.setTarget(opening ? 1f : 0f);
 * open.update();
 * float alpha = open.getValue();
 * }</pre>
 */
public final class Animation {

    private float value;
    private float target;
    private float speed = 0.28f;
    private boolean done;

    public Animation(float initial) {
        this.value = initial;
        this.target = initial;
    }

    public void setTarget(float target) {
        if (this.target != target) {
            this.target = target;
            this.done = false;
        }
    }

    public void snapTo(float value) {
        this.value = value;
        this.target = value;
        this.done = true;
    }

    /** Quanto maior, mais rapido. 0.05 = lento, 0.5 = rapido. */
    public void setSpeed(float speed) {
        this.speed = Math.max(0.01f, speed);
    }

    public void update() {
        if (done) {
            return;
        }
        value += (target - value) * speed;
        if (Math.abs(target - value) < 0.001f) {
            value = target;
            done = true;
        }
    }

    public float getValue() {
        return value;
    }

    public float getTarget() {
        return target;
    }

    public boolean isDone() {
        return done;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }
}
