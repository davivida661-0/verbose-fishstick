package com.solarclient.mods.render;

import com.solarclient.event.Events;
import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.Theme;
import com.solarclient.util.ChatUtils;
import com.solarclient.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 * NameTags.
 *
 * <p>Placa customizada acima de cada jogador: nome, barra de vida, couraça e
 * distancia. Tudo em OpenGL, usando a mesma matematica de projecao que o
 * proprio jogo usa para desenhar as placas nativas.</p>
 *
 * <p>Detalhe importante do 1.8.9: o jogo desenha o nome dentro de um espaco
 * com Y invertido e escala {@code -0.02666}. Por isso o codigo inverte o Y ao
 * desenhar e usa a fonte do vanilla (que ja sabeConviver com esse espaco).</p>
 *
 * <p><b>So informacao que o jogo ja mostra.</b> Nao ha box, ESP, distancia
 * atraves de parede nem nada disso.</p>
 */
public final class NameTags extends Mod {

    private static final float SCALE = 0.0266f;

    public NameTags() {
        super("NameTags", "Placa de nome customizada (vida, couraça, distancia)",
                ModCategory.RENDER, Keyboard.KEY_N);
        values.set("health", true);
        values.set("armor", true);
        values.set("distance", false);
        values.set("maxDistance", 128);
        values.set("showSelf", false);
        values.set("background", true);
    }

    @Override
    public void onRender3D(Events.Render3D event) {
        Minecraft mc = event.mc;
        if (!RenderHelper.inWorld()) {
            return;
        }
        RenderManager manager = RenderHelper.manager();
        if (manager == null) {
            return;
        }

        EntityPlayerSP camera = mc.getRenderViewEntity();
        float camYaw = camera.rotationYaw;
        float camPitch = camera.rotationPitch;
        double maxDistance = values.getInt("maxDistance", 128);

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity.isDead) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living == mc.thePlayer && !values.getBoolean("showSelf", false)) {
                continue;
            }
            if (living instanceof EntityPlayer && living.hurtTime < 0) {
                continue;
            }

            // posicao interpolada (evita o nome "pular" quando o alvo se move)
            double x = living.lastTickPosX + (living.posX - living.lastTickPosX) * event.partialTicks;
            double y = living.lastTickPosY + (living.posY - living.lastTickPosY) * event.partialTicks;
            double z = living.lastTickPosZ + (living.posZ - living.lastTickPosZ) * event.partialTicks;

            double distance = mc.thePlayer.getDistanceToEntity(living);
            if (distance > maxDistance) {
                continue;
            }

            float labelY = (float) (y + living.getEyeHeight() + 0.35 - manager.renderPosY);
            GL11.glPushMatrix();
            GL11.glTranslatef((float) (x - manager.renderPosX), labelY, (float) (z - manager.renderPosZ));
            GL11.glRotatef(180f - camYaw, 0f, 1f, 0f);
            GL11.glRotatef(-camPitch, 1f, 0f, 0f);
            GL11.glScalef(-SCALE, -SCALE, SCALE);

            drawTag(mc, living, distance);
            GL11.glPopMatrix();
        }

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    private void drawTag(Minecraft mc, EntityLivingBase living, double distance) {
        String name = ChatUtils.getUnformattedText(living.getDisplayName());
        int fontWidth = mc.fontRenderer.getStringWidth(name);
        int width = fontWidth + 8;
        int height = values.getBoolean("health", true) ? 22 : 12;

        if (values.getBoolean("background", true)) {
            RenderUtils.roundedRect(-width / 2f, -height, width, height, 4f,
                    Colors.argb(140, 10, 12, 18));
        }

        // nome (a fonte do vanilla e' a unica que respeita o espaco invertido)
        mc.fontRenderer.drawString(name, -fontWidth / 2, -height + 2, Theme.getAccent(), true);

        if (values.getBoolean("health", true)) {
            float health = living.getHealth() / Math.max(1f, living.getMaxHealth());
            int barWidth = width - 4;
            // no espaco invertido o Y cresce para baixo, entao a barra e desenhada
            // de baixo para cima
            RenderUtils.roundedRect(-barWidth / 2f, -10f, barWidth, 3f, 1.5f,
                    Colors.argb(90, 0, 0, 0));
            RenderUtils.roundedRect(-barWidth / 2f, -10f, barWidth * health, 3f, 1.5f,
                    health > 0.5f ? Colors.rgb(0x3E, 0xE0, 0xA4)
                            : health > 0.25f ? Theme.WARNING : Theme.DANGER);
        }

        if (values.getBoolean("armor", true)) {
            // 20 pontos = armadura cheia; desenhamos um numero simples ao lado
            int armor = living instanceof EntityPlayer
                    ? ((EntityPlayer) living).getTotalArmorValue() : 0;
            String text = String.valueOf(armor);
            mc.fontRenderer.drawString(text, width / 2f - mc.fontRenderer.getStringWidth(text) - 2f,
                    -height + 2, Colors.rgb(0xBF, 0xD4, 0xF5), true);
        }

        if (values.getBoolean("distance", false)) {
            String text = String.format("%.1fm", distance);
            mc.fontRenderer.drawString(text,
                    -mc.fontRenderer.getStringWidth(text) / 2, 2, Theme.TEXT_SECONDARY, true);
        }
    }
}
