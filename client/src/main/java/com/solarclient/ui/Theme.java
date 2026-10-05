package com.solarclient.ui;

/**
 * <h1>Tema</h1>
 *
 * <p>Tema dark com cor de destaque customizavel (Roxo ou Azul, como pedido),
 * mais as cores de fundo/painel/texto usadas em todas as telas.</p>
 *
 * <p>A cor de destaque e salva no {@code config.json} e pode ser trocada em
 * runtime pela aba Settings do ClickGUI.</p>
 */
public final class Theme {

    /** Presets disponiveis. */
    public enum Preset {
        PURPLE(0xFF7C5CFF, "Roxo"),
        BLUE(0xFF3BA9FF, "Azul"),
        MINT(0xFF3EE0A4, "Menta"),
        SUNSET(0xFFFF6B6B, "Pore do Sol");

        private final int accent;
        private final String displayName;

        Preset(int accent, String displayName) {
            this.accent = accent;
            this.displayName = displayName;
        }

        public int getAccent() {
            return accent;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ------------------------------------------------------------- paleta base
    public static final int BACKGROUND = 0xFF0B0C12;
    public static final int SIDEBAR = 0xFF111320;
    public static final int PANEL = 0xFF151827;
    public static final int PANEL_HOVER = 0xFF1C2032;
    public static final int OUTLINE = 0xFF262B3D;
    public static final int TEXT_PRIMARY = 0xFFF2F4F8;
    public static final int TEXT_SECONDARY = 0xFF9AA1B1;
    public static final int SUCCESS = 0xFF3EE0A4;
    public static final int DANGER = 0xFFFF5C5C;
    public static final int WARNING = 0xFFFFC857;

    private static Preset preset = Preset.PURPLE;
    private static int accent = Preset.PURPLE.accent;

    private Theme() {
    }

    public static int getAccent() {
        return accent;
    }

    /** Escurece a cor de destaque (usado em bordas e estados pressionados). */
    public static int getAccentDark() {
        return Colors.darker(accent, 0.55f);
    }

    public static int getAccentSoft() {
        return Colors.withAlpha(accent, 0.18f);
    }

    public static void setAccent(int argb) {
        accent = argb;
    }

    public static void setPreset(Preset preset) {
        Theme.preset = preset;
        accent = preset.getAccent();
    }

    public static Preset getPreset() {
        return preset;
    }
}
