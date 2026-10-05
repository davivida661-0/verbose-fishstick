package com.solarclient.mod;

import com.solarclient.event.Events;
import com.solarclient.util.Logger;
import net.minecraft.client.settings.KeyBinding;

/**
 * <h1>Mod base</h1>
 *
 * <p>Todo modulo do client extends esta classe. Ela cuida de:</p>
 * <ul>
 *     <li>estado ligado/desligado (com log);</li>
 *     <li>atalho de teclado (criado a partir do vanilla {@link KeyBinding}, ou
 *         seja aparece de graca no menu de controles do jogo);</li>
 *     <li>posicao/escala do HUD (usado pelo HUD Editor e salvo no JSON);</li>
 *     <li>os hooks {@link #onTick()}, {@link #onRender2D(Events.Render2D)} e
 *         {@link #onRender3D(Events.Render3D)}.</li>
 * </ul>
 *
 * <p><b>Regra do projeto:</b> mod aqui nunca altera pacote nem movimento do
 * jogador. So observacao (render, HUD, animacao visual local) e QoL.</p>
 */
public abstract class Mod {

    /** Categoria mostrada no menu de controles do Minecraft. */
    public static final String KEY_CATEGORY = "Solar Client";

    private final String name;
    private final String description;
    private final ModCategory category;
    /** Configuracoes internas do mod (lidas no construtor para definir defaults). */
    protected final ModSettings values = new ModSettings();

    private boolean enabled;
    /** true para mods que desenham no HUD (participam do HUD Editor). */
    protected boolean hud;

    // Posicao/escala do HUD (em pixels ja escalados pela resolucao do jogo)
    public int x;
    public int y;
    public float scale = 1.0f;

    // Tecla
    private final boolean hasKey;
    private final int defaultKey;
    private KeyBinding keyBinding;

    protected Mod(String name, String description, ModCategory category) {
        this(name, description, category, 0, false);
    }

    protected Mod(String name, String description, ModCategory category, int defaultKey) {
        this(name, description, category, defaultKey, true);
    }

    private Mod(String name, String description, ModCategory category, int defaultKey, boolean hasKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultKey = defaultKey;
        this.hasKey = hasKey;
    }

    // ------------------------------------------------------------------ getters
    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final ModCategory getCategory() {
        return category;
    }

    public final ModSettings getValues() {
        return values;
    }

    public final boolean isHud() {
        return hud;
    }

    // ------------------------------------------------------------------ estado
    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        Logger.info("Mod " + name + (enabled ? " LIGADO" : " DESLIGADO"));
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    // ------------------------------------------------------------------ tecla
    /**
     * Cria a KeyBinding do mod. Precisa ser chamado DEPOIS do config ter sido
     * aplicado (o valor da tecla vem do JSON).
     */
    public final void linkKeyBinding() {
        if (!hasKey) {
            return;
        }
        this.keyBinding = new KeyBinding(name, values.getInt("key", defaultKey), KEY_CATEGORY);
    }

    public final KeyBinding getKeyBinding() {
        return keyBinding;
    }

    public final int getKeyCode() {
        return keyBinding == null ? 0 : keyBinding.keyCode;
    }

    /** Troca a tecla e ja persiste no config JSON. */
    public final void setKeyCode(int keyCode) {
        values.set("key", keyCode);
        if (keyBinding != null) {
            keyBinding.keyCode = keyCode;
        }
    }

    public final int getDefaultKeyCode() {
        return defaultKey;
    }

    // ------------------------------------------------------------------ posicao
    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public final float getScale() {
        return scale;
    }

    public final void setScale(float scale) {
        // 0.5x .. 3.0x: abaixo disso o texto fica ilegivel, acima some da tela
        this.scale = Math.max(0.5f, Math.min(3.0f, scale));
    }

    // ------------------------------------------------------------------ hooks
    /** 20x por segundo. */
    public void onTick() {
    }

    /** Camada HUD (coordenadas ja escaladas por {@code ScaledResolution}). */
    public void onRender2D(Events.Render2D event) {
    }

    /** Camada do mundo 3D. */
    public void onRender3D(Events.Render3D event) {
    }
}
