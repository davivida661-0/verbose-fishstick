package com.solarclient.mod;

import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import org.lwjgl.opengl.GL11;

/**
 * <h1>Mod de HUD</h1>
 *
 * <p>Troca de classe para quem desenha no HUD. Ganha de graca:</p>
 * <ul>
 *     <li>posicao/escala e o transform OpenGL (a escala fica "grudada" no
 *         desenho, entao o mod nunca precisa multiplicar coordenada por mao);</li>
 *     <li>{@code setSize(w, h)} no fim do desenho, que e o que o HUD Editor usa
 *         para saber a area de arraste;</li>
 *     <li>atalho padrao: comeca no canto inferior direito (area dos CEOs).</li>
 * </ul>
 *
 * <p>Exemplo minimo:</p>
 * <pre>{@code
 * public final class MeuHud extends HudMod {
 *     public MeuHud() { super("MeuHud", "descricao", ModCategory.COMBAT, 0, 0, Keyboard.KEY_R); }
 *
 *     @Override
 *     public void onRender2D(Events.Render2D e) {
 *         begin();                                  // aplica x/y/scale
 *         drawText("OL", 0, 0, Colors.WHITE);
 *         setSize(20, 10);                          // area para arrastar
 *         end();                                    // desfaz o transform
 *     }
 * }
 * }</pre>
 */
public abstract class HudMod extends Mod {

    private int width;
    private int height;
    private final int defaultX;
    private final int defaultY;

    protected HudMod(String name, String description, ModCategory category,
                     int defaultX, int defaultY, int defaultKey) {
        super(name, description, category, defaultKey);
        this.hud = true;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.x = defaultX;
        this.y = defaultY;
    }

    /** Posicao padrao usada pelo botao "Resetar" do HUD Editor. */
    public final int getDefaultX() {
        return defaultX;
    }

    public final int getDefaultY() {
        return defaultY;
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    /** Chame no fim do {@link #onRender2D} com o tamanho desenhado. */
    protected final void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** Aplica posicao + escala do HUD. Sempre fechar com {@link #end()}. */
    protected final void begin() {
        RenderUtils.begin2D();
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0f);
        GL11.glScalef(scale, scale, 1.0f);
    }

    protected final void end() {
        GL11.glPopMatrix();
        RenderUtils.end2D();
    }

    // ------------------------------------------------------------------ helpers
    /** Painel padrao: fundo escuro, borda suave e cantos arredondados. */
    protected final void drawPanel(int w, int h) {
        drawPanel(w, h, 0.55f, 5.0f);
    }

    protected final void drawPanel(int w, int h, float alpha, float radius) {
        RenderUtils.roundedRect(0, 0, w, h, radius, Colors.argb((int) (alpha * 255), 12, 14, 22));
        RenderUtils.roundedOutline(0, 0, w, h, radius, Colors.argb(70, 255, 255, 255));
    }

    protected final void drawText(String text, float dx, float dy) {
        drawText(text, dx, dy, Colors.WHITE);
    }

    protected final void drawText(String text, float dx, float dy, int color) {
        SolarFont.get().draw(text, dx, dy, color);
    }

    protected final void drawTextShadow(String text, float dx, float dy, int color) {
        SolarFont.get().drawShadow(text, dx, dy, color);
    }

    /** Largura em pixels do texto na fonte atual. */
    protected final int textWidth(String text) {
        return (int) Math.round(SolarFont.get().width(text) * scale);
    }

    protected final int accent() {
        return Theme.getAccent();
    }
}
