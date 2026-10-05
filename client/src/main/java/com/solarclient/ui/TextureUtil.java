package com.solarclient.ui;

import com.solarclient.util.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Carrega PNGs do classpath para texturas OpenGL.
 *
 * <p>Usado pelos cosmeticos (capas, asas, chapeu) - o 1.8.9 nao tem um
 * carregador de textura "de mod", entao o client faz o seu a partir das
 * classes do Java.</p>
 *
 * <p>Formato esperado: PNG com alpha, 64x32 para capas (igual ao formato
 * vanilla) e 128x128 para asas/chapeu.</p>
 */
public final class TextureUtil {

    private static final Map<String, Integer> CACHE = new HashMap<>();

    private TextureUtil() {
    }

    /**
     * Carrega (uma vez) a textura em {@code /assets/solarclient/textures/<path>}.
     *
     * @return o id da textura OpenGL, ou 0 se nao achou
     */
    public static int load(String path) {
        Integer cached = CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        String resource = "/assets/solarclient/textures/" + path;
        try (InputStream in = TextureUtil.class.getResourceAsStream(resource)) {
            if (in == null) {
                Logger.warn("Textura de cosmetico nao encontrada: " + resource);
                CACHE.put(path, 0);
                return 0;
            }
            BufferedImage image = ImageIO.read(in);
            int id = upload(image);
            CACHE.put(path, id);
            return id;
        } catch (Exception e) {
            Logger.error("Falha ao carregar a textura " + resource, e);
            CACHE.put(path, 0);
            return 0;
        }
    }

    private static int upload(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int p : pixels) {
            buffer.put((byte) ((p >> 16) & 0xFF));
            buffer.put((byte) ((p >> 8) & 0xFF));
            buffer.put((byte) (p & 0xFF));
            buffer.put((byte) ((p >>> 24) & 0xFF));
        }
        buffer.flip();

        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        // NEAREST: preserva o visual "pixelado" de capa, como no vanilla
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return id;
    }
}
