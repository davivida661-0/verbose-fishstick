package com.solarclient.gui;

import com.solarclient.SolarClient;
import com.solarclient.config.ConfigManager;
import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import com.solarclient.mod.ModManager;
import com.solarclient.ui.Animation;
import com.solarclient.ui.Blur;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>ClickGUI</h1>
 *
 * <p>Menu principal do client, no estilo dos clients grandes:</p>
 * <ul>
 *     <li>fundo com blur (o {@link Blur} copia a tela e embaca em um FBO
 *         reduzido) + vinheta escura;</li>
 *     <li>sidebar a esquerda com as categorias e um indicador animado que
 *         "desliza" para a categoria selecionada;</li>
 *     <li>cards de mod a direita: nome, descricao, tecla e interruptor;</li>
 *     <li>aba Settings: tema (Roxo/Azul/...), abrir o HUD Editor, abrir a GUI
 *         de cosmeticos e ver o caminho do config.</li>
 * </ul>
 *
 * <p>Abre com a teclapadilha: clique esquerdo em um card liga/desliga,
 * clique direito abre o HUD Editor para aquele mod.</p>
 */
public final class ClickGUI extends GuiScreen {

    private static final int CARD_WIDTH = 210;
    private static final int CARD_HEIGHT = 48;
    private static final int GAP = 10;
    private static final int SIDEBAR_WIDTH = 150;

    private final Map<String, Animation> switchAnimations = new HashMap<>();
    private final Map<ModCategory, Animation> indicator = new HashMap<>();

    private ModCategory selected = ModCategory.COMBAT;
    private final Animation open = new Animation(0f);
    private float scroll;
    private int lastMouseY;

    @Override
    public void initGui() {
        open.snapTo(0f);
        open.setTarget(1f);
    }

    @Override
    public boolean doesGuiPauseGame() {
        // nao pausa: o jogador ve o mundo ao fundo
        return false;
    }

    // ------------------------------------------------------------------ render
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        open.update();
        float t = open.getValue();

        // fundo: blur do mundo + escurecimento
        Blur.apply(4);
        Blur.scrim(width, height, 0.45f * t);

        RenderUtils.begin2D();
        // painel principal com "pop" na abertura
        float scale = 0.96f + 0.04f * t;
        int panelWidth = Math.round(width * scale);
        int panelHeight = Math.round(height * scale);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        drawSidebar(left, top, panelHeight, mouseX, mouseY, t);
        drawContent(left, top, panelWidth, panelHeight, mouseX, mouseY, partialTicks, t);

        RenderUtils.end2D();

        // fecha a tela: ESC ou clique fora
        if (t > 0.95f && (isClickingOutside(mouseX, mouseY) || shouldClose)) {
            mc.displayGuiScreen(null);
        }
    }

    private boolean shouldClose;

    private void drawSidebar(int left, int top, int panelHeight, int mouseX, int mouseY, float t) {
        RenderUtils.roundedRect(left, top, SIDEBAR_WIDTH, panelHeight, 14f, Theme.SIDEBAR);
        GuiWidgets.brand(width, top + 26);

        float y = top + 48;
        for (ModCategory category : ModCategory.values()) {
            Animation anim = indicator.get(category);
            if (anim == null) {
                anim = new Animation(0f);
                indicator.put(category, anim);
            }
            anim.setTarget(category == selected ? 1f : 0f);
            anim.update();

            boolean hovered = GuiWidgets.isHovered(left + 8, y, SIDEBAR_WIDTH - 16, 32, mouseX, mouseY);
            GuiWidgets.listItem(left + 8, y, SIDEBAR_WIDTH - 16, 32,
                    category.getDisplayName(), category == selected, hovered, mouseX, mouseY);
            y += 36;
        }

        // rodape: versao
        SolarFont.get("Sora", 11, false).draw("v" + SolarClient.VERSION,
                left + 14, top + panelHeight - 16, Colors.argb(90, 255, 255, 255));
    }

    private void drawContent(int left, int top, int panelWidth, int panelHeight,
                             int mouseX, int mouseY, float partialTicks, float t) {
        int contentX = left + SIDEBAR_WIDTH;
        int contentWidth = panelWidth - SIDEBAR_WIDTH;

        RenderUtils.roundedRect(contentX, top, contentWidth, panelHeight, 14f,
                Colors.argb(235, 13, 15, 24));

        SolarFont font = SolarFont.get("Sora", 20, true);
        font.draw(selected.getDisplayName(), contentX + 26, top + 34, Theme.TEXT_PRIMARY);

        if (selected == ModCategory.COSMETICS) {
            drawCosmeticsShortcut(contentX, top, contentWidth, mouseX, mouseY);
            return;
        }
        if (selected == ModCategory.SETTINGS) {
            drawSettings(contentX, top, contentWidth, contentHeight(panelHeight), mouseX, mouseY);
            return;
        }

        ModManager manager = SolarClient.get().modManager;
        List<Mod> mods = new ArrayList<>(manager.byCategory(selected));

        // area rolavel
        int areaX = contentX + 20;
        int areaY = top + 50;
        int areaW = contentWidth - 40;
        int areaH = contentHeight(panelHeight) - 60;

        int columns = Math.max(1, areaW / (CARD_WIDTH + GAP));
        RenderUtils.scissor(areaX, areaY - 4, areaW, areaH + 8);
        RenderUtils.begin2D();

        for (int i = 0; i < mods.size(); i++) {
            Mod mod = mods.get(i);
            int col = i % columns;
            int row = i / columns;
            float x = areaX + col * (CARD_WIDTH + GAP);
            float y = areaY + row * (CARD_HEIGHT + GAP) - scroll;

            if (y > areaY + areaH || y + CARD_HEIGHT < areaY - GAP) {
                continue; // fora da area visivel: nao desenha
            }

            Animation anim = switchAnimations.get(mod.getName());
            if (anim == null) {
                anim = new Animation(mod.isEnabled() ? 1f : 0f);
                switchAnimations.put(mod.getName(), anim);
            }
            anim.setTarget(mod.isEnabled() ? 1f : 0f);

            boolean hovered = GuiWidgets.isHovered(x, y, CARD_WIDTH, CARD_HEIGHT, mouseX, mouseY);
            GuiWidgets.modCard(x, y, CARD_WIDTH, CARD_HEIGHT, mod.getName(), mod.getDescription(),
                    mod.isEnabled(), hovered, anim);

            // tecla do atalho, no canto inferior direito
            String key = keyName(mod);
            if (!key.isEmpty()) {
                SolarFont.get("Sora", 11, false).drawRight(key,
                        x + CARD_WIDTH - 54, y + CARD_HEIGHT - 12, Theme.TEXT_SECONDARY);
            }
        }

        RenderUtils.end2D();
        RenderUtils.endScissor();

        // barra de rolagem
        int totalHeight = ((mods.size() + columns - 1) / columns) * (CARD_HEIGHT + GAP);
        if (totalHeight > areaH) {
            float progress = Math.max(0f, Math.min(1f, scroll / (float) (totalHeight - areaH)));
            GuiWidgets.scrollbar(contentX + contentWidth - 14, areaY, areaH, progress);
        }
    }

    private int contentHeight(int panelHeight) {
        return panelHeight - 50;
    }

    private void drawCosmeticsShortcut(int contentX, int top, int contentWidth,
                                       int mouseX, int mouseY) {
        SolarFont.get("Sora", 14, false).draw(
                "Gerencie capas, asas e chapeus desbloqueados pela API.",
                contentX + 26, top + 60, Theme.TEXT_SECONDARY);
        if (GuiWidgets.button(contentX + 26, top + 80, 180, 32, "Abrir cosmeticos",
                mouseX, mouseY, true)) {
            mc.displayGuiScreen(new CosmeticsGui());
        }
    }

    private void drawSettings(int contentX, int top, int contentWidth, int contentHeight,
                              int mouseX, int mouseY) {
        ConfigManager config = SolarClient.get().configManager;
        SolarFont font = SolarFont.get("Sora", 14, false);
        int y = top + 60;

        font.draw("Cor principal", contentX + 26, y, Theme.TEXT_PRIMARY);
        y += 18;
        int x = contentX + 26;
        for (Theme.Preset preset : Theme.Preset.values()) {
            boolean hover = GuiWidgets.isHovered(x, y, 88, 28, mouseX, mouseY);
            boolean active = Theme.getPreset() == preset;
            RenderUtils.roundedRect(x, y, 88, 28, 8f,
                    active ? Colors.withAlpha(preset.getAccent(), 0.25f)
                            : (hover ? Theme.PANEL_HOVER : Theme.PANEL));
            RenderUtils.roundedOutline(x, y, 88, 28, 8f,
                    active ? preset.getAccent() : Colors.withAlpha(Theme.OUTLINE, 0xB0));
            SolarFont.get("Sora", 12, active).drawCentered(preset.getDisplayName(),
                    x + 44, y + 18, active ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
            x += 96;
        }

        y += 46;
        font.draw("Ferramentas", contentX + 26, y, Theme.TEXT_PRIMARY);
        y += 18;
        if (GuiWidgets.button(contentX + 26, y, 180, 32, "HUD Editor", mouseX, mouseY, false)) {
            mc.displayGuiScreen(new HudEditorGui());
        }
        if (GuiWidgets.button(contentX + 216, y, 180, 32, "Cosmeticos", mouseX, mouseY, false)) {
            mc.displayGuiScreen(new CosmeticsGui());
        }

        y += 42;
        font.draw("Arquivos", contentX + 26, y, Theme.TEXT_PRIMARY);
        y += 18;
        SolarFont.get("Sora", 11, false).draw(config.getPath(), contentX + 26, y,
                Theme.TEXT_SECONDARY);

        y += 24;
        SolarFont.get("Sora", 11, false).draw(
                SolarClient.get().modManager.enabledCount() + " de "
                        + SolarClient.get().modManager.size() + " mods ligados",
                contentX + 26, y, Theme.TEXT_SECONDARY);
    }

    // ------------------------------------------------------------------ input
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        shouldClose = false;

        if (buttonY(mouseX, mouseY)) {
            Mod mod = modAt(mouseX, mouseY);
            if (mod == null) {
                return;
            }
            if (mouseButton == 0) {
                SolarClient.get().modManager.toggle(mod);
                SolarClient.get().configManager.save();
            } else if (mouseButton == 1) {
                // botao direito: abre o HUD Editor focado nesse mod
                mc.displayGuiScreen(new HudEditorGui());
            }
            return;
        }

        if (buttonX(mouseX, mouseY) && mouseButton == 0) {
            ModCategory[] categories = ModCategory.values();
            for (int i = 0; i < categories.length; i++) {
                if (categoryAt(mouseY) == categories[i]) {
                    selected = categories[i];
                    return;
                }
            }
        }

        if (isClickingOutside(mouseX, mouseY)) {
            shouldClose = true;
        }
    }

    private Mod modAt(int mouseX, int mouseY) {
        ModCategory category = categoryAt(mouseY);
        if (category != selected || selected == ModCategory.SETTINGS
                || selected == ModCategory.COSMETICS) {
            return null;
        }
        List<Mod> mods = SolarClient.get().modManager.byCategory(category);
        int areaX = centerLeft() + SIDEBAR_WIDTH + 20;
        int areaY = centerTop() + 50;
        int columns = Math.max(1, (width - SIDEBAR_WIDTH - 40) / (CARD_WIDTH + GAP));
        for (int i = 0; i < mods.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            float x = areaX + col * (CARD_WIDTH + GAP);
            float y = areaY + row * (CARD_HEIGHT + GAP) - scroll;
            if (GuiWidgets.isHovered(x, y, CARD_WIDTH, CARD_HEIGHT, mouseX, mouseY)) {
                return mods.get(i);
            }
        }
        return null;
    }

    private ModCategory categoryAt(int mouseY) {
        float y = centerTop() + 48;
        ModCategory[] categories = ModCategory.values();
        for (ModCategory category : categories) {
            if (mouseY >= y && mouseY <= y + 32) {
                return category;
            }
            y += 36;
        }
        return selected;
    }

    private boolean buttonX(int mouseX, int mouseY) {
        int left = centerLeft();
        return mouseX >= left + 8 && mouseX <= left + SIDEBAR_WIDTH - 8;
    }

    private boolean buttonY(int mouseX, int mouseY) {
        return mouseX > centerLeft() + SIDEBAR_WIDTH && mouseY > centerTop() + 40;
    }

    private boolean isClickingOutside(int mouseX, int mouseY) {
        return mouseX < 0 || mouseY < 0 || mouseX > width || mouseY > height;
    }

    private int centerLeft() {
        return (width - width) / 2;
    }

    private int centerTop() {
        return (height - height) / 2;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int mouseY = Mouse.getY() * height / org.lwjgl.opengl.Display.getHeight();
        int delta = mouseY - lastMouseY;
        lastMouseY = mouseY;

        if (buttonY(Mouse.getX() * width / org.lwjgl.opengl.Display.getWidth(), mouseY)
                && Mouse.isButtonDown(0)) {
            scroll -= delta;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private static String keyName(Mod mod) {
        int key = mod.getKeyCode();
        if (key == 0) {
            return "";
        }
        String name = org.lwjgl.input.Keyboard.getKeyName(key);
        return name == null ? "" : name.toUpperCase();
    }
}
