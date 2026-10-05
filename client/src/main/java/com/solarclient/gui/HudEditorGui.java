package com.solarclient.gui;

import com.solarclient.SolarClient;
import com.solarclient.config.ConfigManager;
import com.solarclient.mod.HudMod;
import com.solarclient.ui.Blur;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * <h1>HUD Editor</h1>
 *
 * <p>Tela de arrasto dos elementos do HUD, no estilo Lunar/Badlion:</p>
 * <ul>
 *     <li>cada mod de HUD e desenhado exatamente como aparece no jogo, com uma
 *         borda tracejada e o nome em cima quando selecionado;</li>
 *     <li><b>arrastar</b> com o botao esquerdo move o elemento (o offset segue
 *         o mouse, e o valor e salvo em {@link #mod.getX()}/{@link #mod.getY()});</li>
 *     <li><b>snap</b>: ao soltar perto de uma borda ou do centro, o elemento
 *         gruda (grade de {@link ConfigManager#getSnapGrid()} px);</li>
 *     <li><b>escala</b>: roda do mouse sobre o elemento selecionado (0.5x a
 *         3.0x);</li>
 *     <li>botões: Resetar layout, Alternar snap e Concluir (salva o JSON).</li>
 * </ul>
 *
 * <p>Por que desenhar os mods aqui: eles so sabem o proprio tamanho depois
 * de renderizar, entao o editor desenha o frame normal e usa o
 * {@code setSize()} que cada mod preencheu como area de arraste.</p>
 */
public final class HudEditorGui extends GuiScreen {

    private HudMod dragging;
    private HudMod selected;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean snap = true;

    @Override
    public void initGui() {
        snap = SolarClient.get().configManager.isSnapEnabled();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ------------------------------------------------------------------ render
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int scaledMouseX = Mouse.getX() * width / org.lwjgl.opengl.Display.getWidth();
        int scaledMouseY = Mouse.getY() * height / org.lwjgl.opengl.Display.getHeight();

        Blur.scrim(width, height, 0.35f);

        // -------------------------------------------------------------- HUD
        // forca os mods de HUD a desenharem mesmo desligados enquanto edita
        List<HudMod> mods = SolarClient.get().modManager.hudMods();
        for (HudMod mod : mods) {
            boolean wasEnabled = mod.isEnabled();
            if (!wasEnabled) {
                mod.setEnabled(true);
            }
            mod.onRender2D(new com.solarclient.event.Events.Render2D(
                    width, height, scaledMouseX, scaledMouseY, partialTicks));
            if (!wasEnabled) {
                mod.setEnabled(false);
            }
        }

        // ---------------------------------------------------------- molduras
        RenderUtils.begin2D();
        for (HudMod mod : mods) {
            float w = mod.getWidth();
            float h = mod.getHeight();
            if (w <= 0 || h <= 0) {
                continue;
            }
            boolean isSelected = mod == selected;
            int color = isSelected ? Theme.getAccent() : Colors.argb(70, 255, 255, 255);

            RenderUtils.roundedOutline(mod.getX(), mod.getY(), w, h, 3f, color);
            // cantoneiras destacadas quando selecionado
            if (isSelected) {
                for (int i = 0; i < 4; i++) {
                    float cx = (i == 0 || i == 3) ? mod.getX() : mod.getX() + w;
                    float cy = (i < 2) ? mod.getY() : mod.getY() + h;
                    RenderUtils.circle(cx, cy, 3f, Theme.getAccent());
                }
                SolarFont.get("Sora", 11, false).drawCentered(mod.getName(),
                        mod.getX() + w / 2f, mod.getY() - 6, Theme.getAccent());
            }
        }

        // ------------------------------------------------------------ guias
        drawGuides(scaledMouseX, scaledMouseY);

        // ------------------------------------------------------------ topbar
        drawTopBar(mouseX, mouseY);
        RenderUtils.end2D();

        drawHint(scaledMouseX, scaledMouseY);
    }

    /** Linhas de centro (para o snap ficar obvia). */
    private void drawGuides(int mouseX, int mouseY) {
        if (!snap) {
            return;
        }
        int color = Colors.argb(30, 255, 255, 255);
        RenderUtils.rect(width / 2f, 0, 1f, height, color);
        RenderUtils.rect(0, height / 2f, width, 1f, color);
    }

    private void drawTopBar(int mouseX, int mouseY) {
        int barWidth = 420;
        int barHeight = 40;
        int x = (width - barWidth) / 2;
        int y = 8;

        RenderUtils.roundedRect(x, y, barWidth, barHeight, 12f, Colors.argb(235, 13, 15, 24));
        RenderUtils.roundedOutline(x, y, barWidth, barHeight, 12f, Colors.withAlpha(Theme.OUTLINE, 0xC0));

        SolarFont font = SolarFont.get("Sora", 13, false);
        font.draw("HUD Editor", x + 16, y + 17, Theme.TEXT_PRIMARY);
        font.draw(snap ? "snap: ligado" : "snap: desligado", x + 16, y + 31, Theme.TEXT_SECONDARY);

        if (GuiWidgets.button(x + 130, y + 9, 90, 24, "Snap", mouseX, mouseY, false)) {
            snap = !snap;
            SolarClient.get().configManager.setSnapEnabled(snap);
        }
        if (GuiWidgets.button(x + 226, y + 9, 90, 24, "Resetar", mouseX, mouseY, false)) {
            resetLayout();
        }
        if (GuiWidgets.button(x + 322, y + 9, 86, 24, "Concluir", mouseX, mouseY, true)) {
            SolarClient.get().configManager.save();
            mc.displayGuiScreen(null);
        }
    }

    private void drawHint(int mouseX, int mouseY) {
        if (selected == null) {
            SolarFont.get("Sora", 12, false).drawCentered(
                    "Arraste um elemento para mover | roda do mouse muda o tamanho | ESC sai",
                    width / 2f, height - 24, Colors.argb(160, 255, 255, 255));
            return;
        }
        SolarFont.get("Sora", 12, false).drawCentered(
                selected.getName() + "  -  " + Math.round(selected.getX()) + ", "
                        + Math.round(selected.getY()) + "  -  " + String.format("%.1fx", selected.getScale()),
                width / 2f, height - 24, Colors.argb(200, 255, 255, 255));
    }

    // ------------------------------------------------------------------ input
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (HudMod mod : SolarClient.get().modManager.hudMods()) {
            if (mod.getWidth() <= 0) {
                continue;
            }
            if (GuiWidgets.isHovered(mod.getX(), mod.getY(), mod.getWidth(), mod.getHeight(),
                    mouseX, mouseY)) {
                selected = mod;
                dragging = mod;
                dragOffsetX = mouseX - mod.getX();
                dragOffsetY = mouseY - mod.getY();
                return;
            }
        }
        selected = null;
        dragging = null;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (dragging != null) {
            dragging = null;
            // salva assim que solta: o layout nao se perde se o jogo fechar
            SolarClient.get().configManager.save();
        }
    }

    @Override
    public void handleMouseInput() {
        int mouseX = Mouse.getX() * width / org.lwjgl.opengl.Display.getWidth();
        int mouseY = Mouse.getY() * height / org.lwjgl.opengl.Display.getHeight();

        if (dragging != null && Mouse.isButtonDown(0)) {
            applySnap(dragging, mouseX - dragOffsetX, mouseY - dragOffsetY);
        }

        // roda do mouse sobre o elemento selecionado
        if (selected != null && Mouse.isButtonDown(0) == false) {
            int wheel = Mouse.getDWheel();
            if (wheel != 0) {
                if (GuiWidgets.isHovered(selected.getX(), selected.getY(), selected.getWidth(),
                        selected.getHeight(), mouseX, mouseY)) {
                    selected.setScale(selected.getScale() + (wheel > 0 ? 0.05f : -0.05f));
                    SolarClient.get().configManager.save();
                }
            }
        }
        super.handleMouseInput();
    }

    /** Puxa o elemento para a grade e para as bordas quando estiver perto. */
    private void applySnap(HudMod mod, float x, float y) {
        if (!snap) {
            mod.setPosition(Math.round(x), Math.round(y));
            return;
        }
        int grid = SolarClient.get().configManager.getSnapGrid();

        // bordas da tela
        float threshold = 6f;
        if (x < threshold) {
            x = 0;
        } else if (x + mod.getWidth() > width - threshold) {
            x = width - mod.getWidth();
        }
        if (y < threshold) {
            y = 0;
        } else if (y + mod.getHeight() > height - threshold) {
            y = height - mod.getHeight();
        }

        // centro
        if (Math.abs(x + mod.getWidth() / 2f - width / 2f) < threshold) {
            x = width / 2f - mod.getWidth() / 2f;
        }
        if (Math.abs(y + mod.getHeight() / 2f - height / 2f) < threshold) {
            y = height / 2f - mod.getHeight() / 2f;
        }

        // grade
        mod.setPosition(Math.round(x / grid) * grid, Math.round(y / grid) * grid);
    }

    private void resetLayout() {
        // reconstroi os mods: o construtor volta com a posicao padrao
        com.solarclient.mod.ModManager manager = SolarClient.get().modManager;
        for (HudMod mod : manager.hudMods()) {
            mod.setPosition(mod.getDefaultX(), mod.getDefaultY());
            mod.setScale(1f);
        }
        SolarClient.get().configManager.save();
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            SolarClient.get().configManager.save();
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
