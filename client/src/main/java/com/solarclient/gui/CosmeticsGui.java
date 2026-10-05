package com.solarclient.gui;

import com.solarclient.SolarClient;
import com.solarclient.cosmetics.Cosmetic;
import com.solarclient.cosmetics.CosmeticManager;
import com.solarclient.cosmetics.CosmeticType;
import com.solarclient.cosmetics.Rarity;
import com.solarclient.ui.Animation;
import com.solarclient.ui.Blur;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import com.solarclient.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <h1>GUI de Cosmeticos</h1>
 *
 * <p>Tela com:</p>
 * <ul>
 *     <li><b>preview 3D</b> do jogador girando devagar (usa o proprio
 *         {@code RenderPlayer} do jogo, entao a capa aparece exatamente como
 *         os outros jogadores veem);</li>
 *     <li>lista de cosmeticos do tipo selecionado, com selo de raridade
 *         colorido;</li>
 *     <li>clique para equipar/desequipar e salvar no servidor pela API.</li>
 * </ul>
 *
 * <p><b>Sobre o preview 3D no 1.8.9:</b> o renderizador de jogador so funciona
 * com um {@code RenderManager} posicionado. Esta tela reaproveita o jogador
 * real (ele fica parado enquanto a tela esta aberta) e desenha ele com a
 * camera da propria tela. Por isso a preview so aparece com o jogo em um
 * mundo carregado - de proposito, para nao ter que criar um jogador falso.</p>
 */
public final class CosmeticsGui extends GuiScreen {

    private static final int CARD_WIDTH = 150;
    private static final int CARD_HEIGHT = 44;

    private CosmeticType selectedType = CosmeticType.CAPE;
    private Cosmetic hovered;
    private final Animation open = new Animation(0f);
    private float spin = 0f;

    @Override
    public void initGui() {
        open.snapTo(0f);
        open.setTarget(1f);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ------------------------------------------------------------------ render
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        open.update();
        Blur.apply(4);
        Blur.scrim(width, height, 0.55f * open.getValue());

        RenderUtils.begin2D();
        // painel
        int panelWidth = Math.min(width - 60, 760);
        int panelHeight = Math.min(height - 60, 420);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        RenderUtils.roundedRect(left, top, panelWidth, panelHeight, 16f, Colors.argb(240, 12, 14, 22));
        RenderUtils.roundedOutline(left, top, panelWidth, panelHeight, 16f,
                Colors.withAlpha(Theme.OUTLINE, 0xC0));

        SolarFont.get("Sora", 20, true).draw("Cosmeticos", left + 24, top + 30, Theme.TEXT_PRIMARY);
        SolarFont.get("Sora", 12, false).draw("Clique para equipar - sincronizado pela API",
                left + 24, top + 46, Theme.TEXT_SECONDARY);

        drawTypeTabs(left + 24, top + 60, mouseX, mouseY);
        drawPlayerPreview(left + 24, top + 100, panelWidth - 300, panelHeight - 130);
        drawList(left + panelWidth - 250, top + 100, 226, panelHeight - 130, mouseX, mouseY);

        RenderUtils.end2D();

        // 3D fica fora do begin2D: precisa da projecao do mundo
        if (open.getValue() > 0.05f) {
            drawPlayer3D(left + 24 + (panelWidth - 300) / 2, top + 100 + (panelHeight - 130) / 2);
        }
    }

    private void drawTypeTabs(int x, int y, int mouseX, int mouseY) {
        int tabX = x;
        for (CosmeticType type : CosmeticType.values()) {
            boolean hover = GuiWidgets.isHovered(tabX, y, 92, 28, mouseX, mouseY);
            boolean active = type == selectedType;
            RenderUtils.roundedRect(tabX, y, 92, 28, 8f,
                    active ? Theme.getAccentSoft() : (hover ? Theme.PANEL_HOVER : Theme.PANEL));
            RenderUtils.roundedOutline(tabX, y, 92, 28, 8f,
                    active ? Theme.getAccent() : Colors.withAlpha(Theme.OUTLINE, 0xA0));
            SolarFont.get("Sora", 12, active).drawCentered(type.getDisplayName(),
                    tabX + 46, y + 18, active ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
            tabX += 98;
        }
    }

    /** Moldura e pedestal da area 3D. */
    private void drawPlayerPreview(int x, int y, int w, int h) {
        RenderUtils.roundedRect(x, y, w, h, 12f, Colors.argb(200, 18, 21, 32));
        RenderUtils.roundedOutline(x, y, w, h, 12f, Colors.withAlpha(Theme.OUTLINE, 0xB0));
        // pedestal
        RenderUtils.circle(x + w / 2f, y + h - 40, 46, Colors.argb(40, 255, 255, 255));
        RenderUtils.circleOutline(x + w / 2f, y + h - 40, 46, Colors.argb(90, 255, 255, 255));

        if (!RenderHelper.inWorld()) {
            SolarFont.get("Sora", 12, false).drawCentered("Entre em um mundo para ver a preview",
                    x + w / 2f, y + h / 2f, Theme.TEXT_SECONDARY);
        }
    }

    private void drawList(int x, int y, int w, int h, int mouseX, int mouseY) {
        CosmeticManager manager = CosmeticManager.get();
        List<Cosmetic> items = manager.catalogOf(selectedType);
        UUID uuid = CosmeticManager.uuidOf(mc);
        Map<CosmeticType, String> loadout = manager.loadout(uuid);
        String equippedId = loadout.get(selectedType);

        RenderUtils.scissor(x, y, w, h);
        RenderUtils.begin2D();

        int cardY = y;
        for (Cosmetic cosmetic : items) {
            boolean hover = GuiWidgets.isHovered(x, cardY, w, CARD_HEIGHT - 6, mouseX, mouseY);
            boolean equipped = cosmetic.getId().equals(equippedId);
            Rarity rarity = cosmetic.getRarity();

            RenderUtils.roundedRect(x, cardY, w, CARD_HEIGHT - 6, 10f,
                    hover ? Theme.PANEL_HOVER : Theme.PANEL);
            RenderUtils.roundedOutline(x, cardY, w, CARD_HEIGHT - 6, 10f,
                    equipped ? Theme.getAccent() : Colors.withAlpha(Theme.OUTLINE, 0xB0));

            // selo de raridade
            RenderUtils.roundedRect(x + 8, cardY + 8, 4, CARD_HEIGHT - 22, 2f, rarity.getColor());
            SolarFont.get("Sora", 13, equipped).draw(cosmetic.getDisplayName(),
                    x + 20, cardY + 19, Theme.TEXT_PRIMARY);
            SolarFont.get("Sora", 11, false).draw(rarity.getDisplayName(),
                    x + 20, cardY + 33, rarity.getColor());

            if (equipped) {
                SolarFont.get("Sora", 11, false).drawRight("equipado", x + w - 10, cardY + 19,
                        Theme.getAccent());
            }
            if (hover) {
                hovered = cosmetic;
            }
            cardY += CARD_HEIGHT;
        }

        if (items.isEmpty()) {
            SolarFont.get("Sora", 12, false).drawCentered("Nada aqui ainda", x + w / 2f, y + 20,
                    Theme.TEXT_SECONDARY);
        }

        RenderUtils.end2D();
        RenderUtils.endScissor();
    }

    // ------------------------------------------------------------------ 3D
    /**
     * Desenha o jogador girando usando o renderizador do proprio jogo.
     *
     * <p>Tecnica: salva o estado do {@code RenderManager}, aponta a camera
     * para o centro da area, desenha o jogador com uma rotacao de yaw
     * animada e restaura tudo. O jogador real e o proprio do mundo (ele fica
     * parado com a tela aberta).</p>
     */
    private void drawPlayer3D(float centerX, float centerY) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager manager = RenderHelper.manager();
        if (manager == null || mc.thePlayer == null) {
            return;
        }
        EntityPlayer player = mc.thePlayer;

        // girando devagar
        spin += 1.4f;

        GL11.glPushMatrix();
        RenderUtils.begin2D();
        RenderUtils.end2D();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        // perspectiva curta, igual o preview de item do inventario
        org.lwjgl.util.glu.GLU.gluPerspective(50f, aspect(), 0.01f, 100f);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 400f);
        GL11.glScalef(90f, 90f, 90f);
        GL11.glRotatef(180f, 1f, 0f, 0f);
        GL11.glRotatef(180f - spin, 0f, 1f, 0f);
        GL11.glTranslatef(0f, -1.62f, 0f);

        // o render do jogador usa a posicao do RenderManager
        manager.updateCameraPosition(0f);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        try {
            manager.cacheActiveRenderInfo(player, false, false, mc.gameSettings.thirdPersonView, false);
            manager.renderEntity(player, player.posX, player.posY, player.posZ,
                    player.rotationYaw, player.rotationPitch, 0);
        } catch (Throwable ignored) {
            // sem mundo valido: a moldura da preview continua visivel
        }

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    private float aspect() {
        return org.lwjgl.opengl.Display.getWidth()
                / (float) Math.max(1, org.lwjgl.opengl.Display.getHeight());
    }

    // ------------------------------------------------------------------ input
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        hovered = null;

        // abas de tipo
        int tabX = left() + 24;
        for (CosmeticType type : CosmeticType.values()) {
            if (GuiWidgets.isHovered(tabX, top() + 60, 92, 28, mouseX, mouseY)) {
                selectedType = type;
                return;
            }
            tabX += 98;
        }

        if (hovered != null) {
            equip(hovered);
        }
    }

    private void equip(Cosmetic cosmetic) {
        UUID uuid = CosmeticManager.uuidOf(mc);
        if (uuid == null) {
            return;
        }
        CosmeticManager manager = CosmeticManager.get();
        boolean equipped = cosmetic.getId().equals(manager.loadout(uuid).get(cosmetic.getType()));

        manager.equip(uuid, cosmetic.getType(), equipped ? null : cosmetic.getId());
        // sincroniza com a API (nao bloqueia a tela)
        manager.api().saveLoadout(uuid, manager.loadout(uuid), () -> {
        });
        // manda para o servidor para os outros jogadores do client verem
        com.solarclient.network.CosmeticNetwork.get().broadcast();
        SolarClient.get().configManager.save();
    }

    private int left() {
        return (width - Math.min(width - 60, 760)) / 2;
    }

    private int top() {
        return (height - Math.min(height - 60, 420)) / 2;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(new ClickGUI());
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
