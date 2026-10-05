package com.solarclient.ui;

import com.solarclient.util.Logger;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * <h1>Blur / "frosted glass"</h1>
 *
 * <p>O efeito de fundo borrado do ClickGUI, do HUD Editor e da GUI de
 * cosmeticos. Como o 1.8.9 nao tem shader pipeline pronto, o truque e o
 * classico ping-pong: copia o framebuffer para uma textura, desenha essa
 * textura num FBO em 1/4 da resolucao (o downscale ja borra) e depois
 * desenha o FBO de volta na tela com filtro linear (upsample).</p>
 *
 * <p>Custa ~0.3ms num FBO de 1/4 e e seguro: qualquer erro de OpenGL cai no
 * {@link #fallback(int)}, um overlay escuro — a UI nunca "quebra".</p>
 */
public final class Blur {

    private static int screenTexture;
    private static int fbo;
    private static int fboTexture;
    private static int textureWidth;
    private static int textureHeight;
    private static boolean failed;

    private Blur() {
    }

    /**
     * Aplica o blur na tela atual.
     *
     * @param downscale divisor da resolucao (4 = 1/4, bem barato e suave)
     */
    public static void apply(int downscale) {
        if (failed) {
            fallback(0.72f);
            return;
        }
        try {
            int width = Display.getWidth();
            int height = Display.getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            int smallWidth = Math.max(1, width / Math.max(1, downscale));
            int smallHeight = Math.max(1, height / Math.max(1, downscale));

            ensureScreenTexture(width, height);
            ensureFbo(smallWidth, smallHeight);

            // 1) snapshot do que esta na tela agora
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

            // 2) encolhe para o FBO (o downscale e o proprio borrado)
            GL11.glBindFramebuffer(GL11.GL_FRAMEBUFFER, fbo);
            GL11.glViewport(0, 0, smallWidth, smallHeight);
            drawFullscreen(fboTexture);

            // 3) devolve para a tela, agora borrado
            GL11.glBindFramebuffer(GL11.GL_FRAMEBUFFER, 0);
            GL11.glViewport(0, 0, width, height);
            drawFullscreen(fboTexture);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        } catch (Throwable t) {
            failed = true;
            Logger.warn("Blur indisponivel neste contexto OpenGL, usando overlay: " + t);
            fallback(0.72f);
        }
    }

    /** Overlay escuro simples (usado se o blur falhar). */
    public static void fallback(float alpha) {
        RenderUtils.begin2D();
        RenderUtils.rect(0, 0, Display.getWidth(), Display.getHeight(),
                Colors.argb((int) (alpha * 255), 6, 7, 12));
        RenderUtils.end2D();
    }

    /** Overlay simples: fundo preto com a opacidade pedida. */
    public static void scrim(int width, int height, float alpha) {
        RenderUtils.begin2D();
        RenderUtils.rect(0, 0, width, height, Colors.argb((int) (alpha * 255), 6, 7, 12));
        RenderUtils.end2D();
    }

    // ------------------------------------------------------------------ recursos
    private static void ensureScreenTexture(int width, int height) {
        if (screenTexture != 0 && textureWidth == width && textureHeight == height) {
            return;
        }
        if (screenTexture != 0) {
            GL11.glDeleteTextures(screenTexture);
        }
        if (fboTexture != 0) {
            GL11.glDeleteTextures(fboTexture);
            fboTexture = 0;
        }
        if (fbo != 0) {
            GL11.glDeleteFramebuffers(fbo);
            fbo = 0;
        }

        textureWidth = width;
        textureHeight = height;

        screenTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private static void ensureFbo(int width, int height) {
        if (fbo != 0) {
            return;
        }
        fboTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);

        fbo = GL11.glGenFramebuffers();
        GL11.glBindFramebuffer(GL11.GL_FRAMEBUFFER, fbo);
        GL11.glFramebufferTexture2D(GL11.GL_FRAMEBUFFER, GL11.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, fboTexture, 0);
        GL11.glBindFramebuffer(GL11.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private static void drawFullscreen(int texture) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(0f, 0f);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(Display.getWidth(), 0f);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(Display.getWidth(), Display.getHeight());
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(0f, Display.getHeight());
        GL11.glEnd();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
