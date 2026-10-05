package com.solarclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * <h1>Desenho 2D</h1>
 *
 * <p>Tudo que o client desenha passa por aqui: retangulos arredondados,
 * gradientes, sombras, circulos, linhas e scissor. Sao funcoes OpenGL
 * "cruas" (immediate mode, igual o proprio 1.8.9 faz) porque o modulo
 * {@code GL_TRIANGLE_FAN} do LWJGL 2 nao existe e o consumidor final nao
 * muda nada em cima disso.</p>
 *
 * <p>Convencao do projeto: quem desenha HUD sempre chama
 * {@link #begin2D()} ... {@link #end2D()}.</p>
 */
public final class RenderUtils {

    private static final int ARC_STEPS = 6;

    private RenderUtils() {
    }

    // ------------------------------------------------------------------ estado
    public static void begin2D() {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    public static void end2D() {
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void color(int argb) {
        GL11.glColor4f(
                Colors.getRed(argb) / 255f,
                Colors.getGreen(argb) / 255f,
                Colors.getBlue(argb) / 255f,
                Colors.getAlpha(argb) / 255f);
    }

    // ------------------------------------------------------------------ formas
    public static void rect(float x, float y, float w, float h, int argb) {
        color(argb);
        GL11.glBegin(GL11.GL_QUADS);
        vertex(x, y);
        vertex(x + w, y);
        vertex(x + w, y + h);
        vertex(x, y + h);
        GL11.glEnd();
    }

    public static void outline(float x, float y, float w, float h, int argb) {
        rect(x, y, w, 1f, argb);
        rect(x, y + h - 1f, w, 1f, argb);
        rect(x, y, 1f, h, argb);
        rect(x + w - 1f, y, 1f, h, argb);
    }

    /** Retangulo com cantos arredondados (fan de triangulos). */
    public static void roundedRect(float x, float y, float w, float h, float radius, int argb) {
        float r = Math.min(radius, Math.min(w, h) / 2f);
        if (r <= 0.5f) {
            rect(x, y, w, h, argb);
            return;
        }
        color(argb);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        arc(x + r, y + r, r, 180f, 270f);
        arc(x + w - r, y + r, r, 270f, 360f);
        arc(x + w - r, y + h - r, r, 0f, 90f);
        arc(x + r, y + h - r, r, 90f, 180f);
        GL11.glEnd();
    }

    public static void roundedOutline(float x, float y, float w, float h, float radius, int argb) {
        float r = Math.min(radius, Math.min(w, h) / 2f);
        if (r <= 0.5f) {
            outline(x, y, w, h, argb);
            return;
        }
        // Aproxima a borda desenhando 4 "fatias" de 1px com o mesmo raio.
        roundedRect(x, y, w, 1f, 0.5f, argb);
        roundedRect(x, y + h - 1f, w, 1f, 0.5f, argb);
        roundedRect(x, y + r, 1f, h - r * 2f, 0.5f, argb);
        roundedRect(x + w - 1f, y + r, 1f, h - r * 2f, 0.5f, argb);
    }

    /** Sombra suave: camadas translucidas em vez de blur real (custa quase nada). */
    public static void shadow(float x, float y, float w, float h, float radius, int argb, int layers) {
        for (int i = layers; i >= 1; i--) {
            float spread = i * 1.2f;
            roundedRect(x - spread, y - spread + 2f, w + spread * 2f, h + spread * 2f,
                    radius + spread, Colors.withAlpha(argb, Colors.getAlpha(argb) / (float) (i + 1)));
        }
    }

    public static void gradient(float x, float y, float w, float h, int from, int to, boolean vertical) {
        GL11.glBegin(GL11.GL_QUADS);
        if (vertical) {
            color(from);
            vertex(x, y);
            vertex(x + w, y);
            color(to);
            vertex(x + w, y + h);
            vertex(x, y + h);
        } else {
            color(from);
            vertex(x, y);
            vertex(x, y + h);
            color(to);
            vertex(x + w, y + h);
            vertex(x + w, y);
        }
        GL11.glEnd();
        // A cor corrente precisa voltar para branco depois do fan.
        color(0xFFFFFFFF);
    }

    public static void circle(float cx, float cy, float radius, int argb) {
        color(argb);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        vertex(cx, cy);
        for (int i = 0; i <= 16; i++) {
            float a = (float) (Math.PI * 2 * i / 16.0);
            vertex(cx + (float) Math.cos(a) * radius, cy + (float) Math.sin(a) * radius);
        }
        GL11.glEnd();
    }

    public static void circleOutline(float cx, float cy, float radius, int argb) {
        color(argb);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 24; i++) {
            float a = (float) (Math.PI * 2 * i / 24.0);
            vertex(cx + (float) Math.cos(a) * radius, cy + (float) Math.sin(a) * radius);
        }
        GL11.glEnd();
    }

    public static void line(float x1, float y1, float x2, float y2, float width, int argb) {
        color(argb);
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        vertex(x1, y1);
        vertex(x2, y2);
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    public static void quad(float x1, float y1, float x2, float y2,
                            float x3, float y3, float x4, float y4, int argb) {
        color(argb);
        GL11.glBegin(GL11.GL_QUADS);
        vertex(x1, y1);
        vertex(x2, y2);
        vertex(x3, y3);
        vertex(x4, y4);
        GL11.glEnd();
    }

    public static void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        color(argb);
        GL11.glBegin(GL11.GL_TRIANGLES);
        vertex(x1, y1);
        vertex(x2, y2);
        vertex(x3, y3);
        GL11.glEnd();
    }

    /** Desenha uma textura 1.8.9 (ITexture) numa area da tela. */
    public static void blit(int textureId, float x, float y, float w, float h) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        vertex(x, y);
        GL11.glTexCoord2f(1f, 0f);
        vertex(x + w, y);
        GL11.glTexCoord2f(1f, 1f);
        vertex(x + w, y + h);
        GL11.glTexCoord2f(0f, 1f);
        vertex(x, y + h);
        GL11.glEnd();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    // ------------------------------------------------------------------ scissor
    /** Recorta a area de desenho. Coordenadas na mesma escala do HUD. */
    public static void scissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        float factorX = Display.getWidth() / (float) Math.max(1, sr.getScaledWidth());
        float factorY = Display.getHeight() / (float) Math.max(1, sr.getScaledHeight());

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                Math.round(x * factorX),
                Display.getHeight() - Math.round((y + height) * factorY),
                Math.round(width * factorX),
                Math.round(height * factorY));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    // ------------------------------------------------------------------ texto vanilla
    /** Escreve usando a fonte do proprio Minecraft (usado no mundo 3D). */
    public static void drawVanillaText(String text, float x, float y, int argb) {
        Minecraft.getMinecraft().fontRenderer.drawString(
                text, x, y, argb & 0x00FFFFFF, true);
    }

    private static void vertex(float x, float y) {
        GL11.glVertex2f(x, y);
    }

    private static void arc(float cx, float cy, float r, float fromDeg, float toDeg) {
        for (int i = 0; i <= ARC_STEPS; i++) {
            float a = (float) Math.toRadians(fromDeg + (toDeg - fromDeg) * i / ARC_STEPS);
            GL11.glVertex2f(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
        }
    }
}
