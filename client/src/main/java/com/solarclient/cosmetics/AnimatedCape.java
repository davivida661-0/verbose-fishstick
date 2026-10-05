package com.solarclient.cosmetics;

import com.solarclient.ui.Colors;
import com.solarclient.ui.TextureUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

/**
 * <h1>COSMETICO DE EXEMPLO - Animated Cape</h1>
 *
 * <p>Capa animada desenhada em 3D nas costas do jogador. Ela aparece
 * <b>para os outros jogadores que tambem usam o Solar Client</b> porque o
 * {@link com.solarclient.network.CosmeticNetwork} avisa o servidor qual
 * cosmetico cada um esta usando (payload customizado do proprio 1.8.9).</p>
 *
 * <h2>Como funciona a malha</h2>
 * <p>O vanilla desenha a capa como um retangulo rigido. Aqui a capa e uma
 * <b>malha de 6x10 segmentos</b> e cada vertice recebe um deslocamento:</p>
 * <pre>
 *   t = 0 (topo, no pescoco)  ->  movimento 0
 *   t = 1 (barriga/pes)       -> movimento maximo
 *   z += sin(tempo*2.2 + t*3.0) * 0.06 * t
 *   x += sin(tempo*1.7 + t*2.2) * 0.03 * t
 * </pre>
 * <p>O resultado e a ondulacao classica de capa: o topo fica preso no
 * pescoco e a barra balanca conforme o jogador anda (a frequencia sobe
 * quando a velocidade do jogador sobe).</p>
 *
 * <h2>Textura</h2>
 * <p>PNG 64x32 em {@code assets/solarclient/textures/cosmetics/capes/}. O
 * tamanho e o mesmo do vanilla, entao qualquer capa do jogo serve.</p>
 *
 * <h2>Ajustes rapidos</h2>
 * <ul>
 *     <li>a capa aparece na frente em vez de tras? troque o sinal em
 *         {@code glRotatef} (180 - yaw) para (yaw);</li>
 *     <li>quer mais movimento? aumente {@code WAVE_AMPLITUDE};</li>
 *     <li>quer que andando balance mais? veja {@link #speedFactor(EntityPlayer)}.</li>
 * </ul>
 */
public final class AnimatedCape extends Cosmetic {

    private static final int COLUMNS = 6;
    private static final int ROWS = 10;
    private static final float WIDTH = 0.7f;   // largura da capa (blocos)
    private static final float HEIGHT = 0.85f;  // altura da capa
    private static final float WAVE_AMPLITUDE = 0.06f;
    private static final String TEXTURE = "cosmetics/capes/solar_cape.png";

    public AnimatedCape(String id, Rarity rarity, String displayName) {
        super(id, CosmeticType.CAPE, rarity, displayName);
    }

    @Override
    public void render(EntityPlayer player, float partialTicks) {
        if (player == null || Minecraft.getMinecraft().thePlayer == null) {
            return;
        }
        int texture = TextureUtil.load(TEXTURE);
        if (texture == 0) {
            return; // textura faltando: nao desenha nada
        }

        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        // o tempo do mundo mantem a animacao sincronizada entre jogadores
        float time = (Minecraft.getMinecraft().theWorld == null
                ? 0f : Minecraft.getMinecraft().theWorld.getTotalWorldTime()) / 20f;
        float speed = speedFactor(player);

        GL11.glPushMatrix();

        // 1) posiciona no pescoco do jogador (modelo 1.8: pes em y=0, pescoco ~1.5)
        GL11.glTranslatef((float) x, (float) y + 1.5f, (float) z);
        // 2) vira a capa para as costas do jogador
        GL11.glRotatef(180f - player.rotationYaw, 0f, 1f, 0f);
        GL11.glRotatef(player.rotationPitch, 1f, 0f, 0f);

        // 3) so aplica a luz depois de posicionar (a normal e calculada aqui)
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        // desenha os dois lados, senao a capa some quando vista por tras
        GL11.glDisable(GL11.GL_CULL_FACE);

        drawMesh(time, speed);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        applyRarityFx(getRarity(), time);
        GL11.glPopMatrix();
    }

    /**
     * Desenha a malha deformada.
     *
     * <p>Para cada linha (de cima para baixo) cria a posicao de cada coluna,
     * aplica a onda e desenha o quad entre a linha atual e a anterior com as
     * coordenadas de textura correspondentes (64x32).</p>
     */
    private void drawMesh(float time, float speed) {
        float waveTime = time * speed;

        for (int row = 0; row < ROWS; row++) {
            float v0 = row / (float) ROWS;
            float v1 = (row + 1) / (float) ROWS;

            for (int col = 0; col < COLUMNS; col++) {
                float u0 = col / (float) COLUMNS;
                float u1 = (col + 1) / (float) COLUMNS;

                Vertex topLeft = vertex(col, row, waveTime);
                Vertex topRight = vertex(col + 1, row, waveTime);
                Vertex bottomLeft = vertex(col, row + 1, waveTime);
                Vertex bottomRight = vertex(col + 1, row + 1, waveTime);

                // normal aproximada: aponta para tras, inclinada pela onda
                float nx = -(topRight.z - topLeft.z) * 4f;
                float ny = 0.25f;
                float nz = -1f;
                GL11.glNormal3f(nx, ny, nz);

                GL11.glBegin(GL11.GL_QUADS);
                tex(u0, v0);
                GL11.glVertex3f(topLeft.x, topLeft.y, topLeft.z);
                tex(u1, v0);
                GL11.glVertex3f(topRight.x, topRight.y, topRight.z);
                tex(u1, v1);
                GL11.glVertex3f(bottomRight.x, bottomRight.y, bottomRight.z);
                tex(u0, v1);
                GL11.glVertex3f(bottomLeft.x, bottomLeft.y, bottomLeft.z);
                GL11.glEnd();
            }
        }
    }

    /** Posicao de um vertice da malha ja com a onda aplicada. */
    private Vertex vertex(int column, int row, float waveTime) {
        float u = column / (float) COLUMNS;
        float v = row / (float) ROWS;

        float x = (u - 0.5f) * WIDTH;
        float y = -v * HEIGHT;
        float z = -0.125f; // atras do corpo do jogador

        // quanto mais embaixo, mais a capa balanca
        float factor = v;
        x += (float) Math.sin(waveTime * 1.7f + v * 2.2f) * 0.03f * factor;
        z += (float) Math.sin(waveTime * 2.2f + v * 3.0f) * WAVE_AMPLITUDE * factor;

        return new Vertex(x, y, z);
    }

    /** Animacao acelera quando o jogador esta andando (usa a velocidade real). */
    private static float speedFactor(EntityPlayer player) {
        double dx = player.posX - player.lastTickPosX;
        double dz = player.posZ - player.lastTickPosZ;
        double movement = Math.sqrt(dx * dx + dz * dz);
        return 1.0f + (float) Math.min(movement * 12.0, 2.5);
    }

    /**
     * Efeito de raridade: uma segunda passada aditiva com a cor da raridade,
     * pulsando devagar.
     */
    void applyRarityFx(Rarity rarity, float time) {
        if (rarity == null || rarity.getGlow() <= 0f) {
            return;
        }
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2f);
        int color = Colors.withAlpha(rarity.getColor(),
                (int) (255 * rarity.getGlow() * 0.25f * pulse));

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f);

        for (int row = 0; row < ROWS; row++) {
            float v0 = row / (float) ROWS;
            float v1 = (row + 1) / (float) ROWS;
            for (int col = 0; col < COLUMNS; col++) {
                float u0 = col / (float) COLUMNS;
                float u1 = (col + 1) / (float) COLUMNS;
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex3f((u0 - 0.5f) * WIDTH, -v0 * HEIGHT, -0.127f);
                GL11.glVertex3f((u1 - 0.5f) * WIDTH, -v0 * HEIGHT, -0.127f);
                GL11.glVertex3f((u1 - 0.5f) * WIDTH, -v1 * HEIGHT, -0.127f);
                GL11.glVertex3f((u0 - 0.5f) * WIDTH, -v1 * HEIGHT, -0.127f);
                GL11.glEnd();
            }
        }

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    /** Vertice da malha. */
    private static final class Vertex {
        private final float x;
        private final float y;
        private final float z;

        private Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static void tex(float u, float v) {
        GL11.glTexCoord2f(u, v);
    }
}
