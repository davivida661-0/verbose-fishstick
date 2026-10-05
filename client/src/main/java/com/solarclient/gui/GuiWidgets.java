package com.solarclient.gui;

import com.solarclient.ui.Animation;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;

/**
 * <h1>Widgets da interface</h1>
 *
 * <p>Componentes desenhados 100% em OpenGL (o jogo 1.8.9 so oferece botao
 * cinza). Tudo aqui e estatico e recebe as coordenadas ja escaladas.</p>
 *
 * <p>Convenção: a funcao desenha e devolve um {@code boolean} quando o widget
 * cuida do clique (a tela decide o que fazer). O "hover" e calculado pela
 * propria funcao, com um retangulo de 1px de folga.</p>
 */
public final class GuiWidgets {

    private GuiWidgets() {
    }

    public static boolean isHovered(float x, float y, float w, float h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    /** Card de modulo no ClickGUI (fundo, nome, descricao, switch). */
    public static void modCard(float x, float y, float w, float h, String name, String description,
                               boolean enabled, boolean hovered, Animation switchAnim) {
        RenderUtils.roundedRect(x, y, w, h, 10f,
                hovered ? Theme.PANEL_HOVER : Theme.PANEL);
        RenderUtils.roundedOutline(x, y, w, h, 10f, Colors.withAlpha(Theme.OUTLINE, 0xB0));

        SolarFont font = SolarFont.get("Sora", 15, false);
        SolarFont small = SolarFont.get("Sora", 12, false);
        font.draw(name, x + 14, y + 20, Theme.TEXT_PRIMARY);
        small.draw(ellipsis(description, 46), x + 14, y + 36, Theme.TEXT_SECONDARY);

        toggleSwitch(x + w - 46, y + h / 2f - 10, switchAnim);
    }

    /** Interruptor animado (0 = desligado, 1 = ligado). */
    public static void toggleSwitch(float x, float y, Animation anim) {
        anim.update();
        float value = anim.getValue();
        float w = 34f;
        float h = 18f;

        int track = Colors.mix(Colors.argb(120, 255, 255, 255), Theme.getAccent(), value);
        RenderUtils.roundedRect(x, y, w, h, h / 2f, track);

        float knobX = x + 2f + value * (w - h);
        RenderUtils.circle(knobX + h / 2f - 2f, y + h / 2f, h / 2f - 2f, Colors.WHITE);
    }

    /** Botao retangular (usado em "Concluir", "Resetar", "Cosmeticos"...). */
    public static boolean button(float x, float y, float w, float h, String text,
                                 int mouseX, int mouseY, boolean primary) {
        boolean hovered = isHovered(x, y, w, h, mouseX, mouseY);
        int background = primary
                ? (hovered ? Colors.lighter(Theme.getAccent(), 0.12f) : Theme.getAccent())
                : (hovered ? Theme.PANEL_HOVER : Theme.PANEL);
        RenderUtils.roundedRect(x, y, w, h, 8f, background);
        RenderUtils.roundedOutline(x, y, w, h, 8f, Colors.withAlpha(Theme.OUTLINE, 0xC0));

        SolarFont font = SolarFont.get("Sora", 13, primary);
        font.drawCentered(text, x + w / 2f, y + h / 2f + 4f,
                primary ? 0xFF0B0C12 : Theme.TEXT_PRIMARY);
        return hovered;
    }

    /** Item de lista (usado na sidebar e na lista de cosmeticos). */
    public static boolean listItem(float x, float y, float w, float h, String text,
                                   boolean selected, boolean hovered, int mouseX, int mouseY) {
        int background = selected
                ? Theme.getAccentSoft()
                : (hovered ? Theme.PANEL_HOVER : Colors.argb(0, 0, 0, 0));
        if (background != 0) {
            RenderUtils.roundedRect(x, y, w, h, 8f, background);
        }
        if (selected) {
            RenderUtils.roundedRect(x, y + 4, 3, h - 8, 2f, Theme.getAccent());
        }
        SolarFont.get("Sora", 14, selected).draw(text, x + 14, y + h / 2f + 4f,
                selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
        return hovered;
    }

    /** Barra de rolagem fina. */
    public static void scrollbar(float x, float y, float height, float progress) {
        RenderUtils.roundedRect(x, y, 3, height, 2f, Colors.argb(40, 255, 255, 255));
        float thumb = Math.max(28f, height * 0.35f);
        float thumbY = y + (height - thumb) * Math.max(0f, Math.min(1f, progress));
        RenderUtils.roundedRect(x, thumbY, 3, thumb, 2f, Colors.argb(140, 255, 255, 255));
    }

    /** Corta o texto com "..." se passar do limite. */
    public static String ellipsis(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    /** Desenha o nome do client no topo da tela. */
    public static void brand(int screenWidth, float y) {
        SolarFont font = SolarFont.get("Sora", 18, true);
        float width = font.width("SOLAR");
        font.draw("SOLAR", screenWidth / 2f - width / 2f, y, Theme.getAccent());
        font.draw("CLIENT", screenWidth / 2f - width / 2f + font.width("SOLAR") + 4, y,
                Theme.TEXT_SECONDARY);
    }
}
