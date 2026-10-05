package com.solarclient.ui;

import com.solarclient.util.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>Fonte customizada (TTF)</h1>
 *
 * <p>O 1.8.9 so tem fonte bitmap (Minecraft font.png) e nao carrega TTF. Aqui
 * o client rasteriza a fonte TTF uma unica vez com AWT, monta um atlas de
 * glifos e desenha em quads OpenGL. Resultado: titulos nitidos em qualquer
 * resolucao, exatamente como o Lunar/Badlion fazem.</p>
 *
 * <p><b>Instalacao da fonte:</b> coloque o arquivo em
 * {@code src/main/resources/assets/solarclient/fonts/}. Por padrao o client
 * procura {@code Sora.ttf} e {@code Sora-Bold.ttf} (Sora e SIL Open Font
 * License, entao pode ser distribuida com o client). Se o arquivo nao existir,
 * cai para a fonte do sistema e o jogo continua funcionando.</p>
 *
 * <p>Como o desenho acontece em coordenadas ja escaladas pelo HUD, quem chama
 * deve multiplicar a largura por {@code mod.getScale()} se precisar
 * posicionar em relacao ao texto.</p>
 */
public final class SolarFont {

    private static final String FONT_FOLDER = "/assets/solarclient/fonts/";
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    private static final int COLUMNS = 16;
    private static final int PADDING = 1;

    private static final Map<String, SolarFont> CACHE = new HashMap<>();

    private final String family;
    private final int size;
    private final boolean bold;

    private final Map<Character, Glyph> glyphs = new HashMap<>();

    private Font awtFont;
    private int textureId = -1;
    private float cellSize;
    private float ascent;
    private float lineHeight;

    private SolarFont(String family, int size, boolean bold) {
        this.family = family;
        this.size = size;
        this.bold = bold;
        loadAwtFont();
        measureGlyphs();
    }

    // ------------------------------------------------------------------ fabrica
    /** Fonte da UI (usada por padrao em todos os mods). */
    public static SolarFont get() {
        return get("Sora", 16, false);
    }

    public static SolarFont bold(int size) {
        return get("Sora", size, true);
    }

    public static SolarFont get(String family, int size, boolean bold) {
        String key = family + '|' + size + '|' + bold;
        SolarFont font = CACHE.get(key);
        if (font == null) {
            font = new SolarFont(family, size, bold);
            CACHE.put(key, font);
        }
        return font;
    }

    // ------------------------------------------------------------------ metrics
    private void loadAwtFont() {
        String file = FONT_FOLDER + family + (bold ? "-Bold" : "") + ".ttf";
        try (InputStream in = SolarFont.class.getResourceAsStream(file)) {
            awtFont = Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception e) {
            // Sem a fonte: usa a do sistema. O client continua, so muda o visual.
            awtFont = new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
            Logger.warn("Fonte '" + file + "' nao encontrada, usando fallback do sistema. "
                    + "Coloque o .ttf em src/main/resources" + FONT_FOLDER);
        }
    }

    /** Mede os glifos sem tocar no OpenGL (pode rodar antes do contexto GL). */
    private void measureGlyphs() {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = probe.createGraphics();
        g.setFont(awtFont.deriveFont(Font.PLAIN, (float) size));
        java.awt.FontMetrics metrics = g.getFontMetrics();
        ascent = metrics.getAscent();
        lineHeight = metrics.getHeight();
        cellSize = size * 2f + PADDING * 2f;
        g.dispose();

        for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
            char ch = (char) c;
            Glyph glyph = new Glyph();
            glyph.advance = metrics.charWidth(ch);
            glyphs.put(ch, glyph);
        }
    }

    // ------------------------------------------------------------------ atlas
    private void ensureTexture() {
        if (textureId != 0) {
            return;
        }
        try {
            buildAtlas();
        } catch (Throwable t) {
            Logger.error("Falha ao criar o atlas da fonte, usando a fonte do Minecraft", t);
            textureId = -1;
        }
    }

    private void buildAtlas() {
        int cells = LAST_CHAR - FIRST_CHAR + 1;
        int rows = (cells + COLUMNS - 1) / COLUMNS;
        int cell = (int) Math.ceil(cellSize);
        int atlasWidth = nextPowerOfTwo(COLUMNS * cell);
        int atlasHeight = nextPowerOfTwo(rows * cell);

        BufferedImage image = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setFont(awtFont.deriveFont(Font.PLAIN, (float) size));
        java.awt.FontMetrics metrics = g.getFontMetrics();

        for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
            char ch = (char) c;
            int index = c - FIRST_CHAR;
            int col = index % COLUMNS;
            int row = index / COLUMNS;
            int ox = col * cell + PADDING;
            int oy = row * cell + PADDING;

            g.drawString(String.valueOf(ch), ox, oy + metrics.getAscent());

            Glyph glyph = glyphs.get(ch);
            glyph.u0 = (float) ox / atlasWidth;
            glyph.v0 = (float) oy / atlasHeight;
            glyph.u1 = (float) (ox + cell - PADDING * 2) / atlasWidth;
            glyph.v1 = (float) (oy + cell - PADDING * 2) / atlasHeight;
            glyph.quadSize = cell - PADDING * 2;
            glyph.advance = metrics.charWidth(ch);
        }
        g.dispose();

        int[] pixels = image.getRGB(0, 0, atlasWidth, atlasHeight, null, 0, atlasWidth);
        ByteBuffer buffer = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * 4);
        for (int p : pixels) {
            buffer.put((byte) ((p >> 16) & 0xFF));
            buffer.put((byte) ((p >> 8) & 0xFF));
            buffer.put((byte) (p & 0xFF));
            buffer.put((byte) ((p >>> 24) & 0xFF));
        }
        buffer.flip();

        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                atlasWidth, atlasHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    // ------------------------------------------------------------------ medidas
    /** Largura do texto em pixels (nao inclui a escala do HUD). */
    public float width(String text) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        float w = 0f;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = glyphs.get(text.charAt(i));
            w += glyph == null ? size * 0.5f : glyph.advance;
        }
        return w;
    }

    public float getAscent() {
        return ascent;
    }

    public float getLineHeight() {
        return lineHeight;
    }

    public int getSize() {
        return size;
    }

    public String getFamily() {
        return family;
    }

    // ------------------------------------------------------------------ desenho
    /**
     * Desenha o texto. {@code y} e a <b>linha de base</b> (o rodape das letras).
     * Chame dentro de {@link RenderUtils#begin2D()}.
     */
    public void draw(String text, float x, float y, int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }
        ensureTexture();
        if (textureId <= 0) {
            // Sem atlas: pelo menos mostra o texto com a fonte do jogo.
            RenderUtils.drawVanillaText(text, x, y - ascent, argb);
            return;
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        float px = x;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            Glyph glyph = glyphs.get(ch);
            if (glyph == null) {
                glyph = glyphs.get('?');
            }
            if (ch != ' ' && glyph != null && glyph.quadSize > 0) {
                float top = y - ascent;
                RenderUtils.color(argb);
                GL11.glBegin(GL11.GL_QUADS);
                tex(glyph.u0, glyph.v0);
                GL11.glVertex2f(px, top);
                tex(glyph.u1, glyph.v0);
                GL11.glVertex2f(px + glyph.quadSize, top);
                tex(glyph.u1, glyph.v1);
                GL11.glVertex2f(px + glyph.quadSize, top + glyph.quadSize);
                tex(glyph.u0, glyph.v1);
                GL11.glVertex2f(px, top + glyph.quadSize);
                GL11.glEnd();
            }
            px += glyph == null ? size * 0.5f : glyph.advance;
        }
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void drawShadow(String text, float x, float y, int argb) {
        draw(text, x + 1f, y + 1f, Colors.withAlpha(0xFF000000, Colors.getAlpha(argb) / 255f * 0.55f));
        draw(text, x, y, argb);
    }

    public void drawCentered(String text, float centerX, float y, int argb) {
        draw(text, centerX - width(text) / 2f, y, argb);
    }

    public void drawRight(String text, float rightX, float y, int argb) {
        draw(text, rightX - width(text), y, argb);
    }

    private static void tex(float u, float v) {
        GL11.glTexCoord2f(u, v);
    }

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }

    /** Um glifo do atlas. */
    private static final class Glyph {
        float advance;
        float quadSize;
        float u0;
        float v0;
        float u1;
        float v1;
    }
}
