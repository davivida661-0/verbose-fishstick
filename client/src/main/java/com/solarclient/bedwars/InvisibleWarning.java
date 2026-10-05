package com.solarclient.bedwars;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.Colors;
import com.solarclient.ui.RenderUtils;
import com.solarclient.ui.SolarFont;
import com.solarclient.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import org.lwjgl.input.Keyboard;

/**
 * <h1>Aviso de jogador invisivel</h1>
 *
 * <p>Em BedWars existe a "runa de invisibilidade" e mods que deixam o
 * jogador invisivel. Este mod <b>avisa</b> (som + texto no HUD) quando algum
 * jogador fica invisivel perto de voce.</p>
 *
 * <p><b>Limites propositais (leia antes de ligar):</b></p>
 * <ul>
 *     <li>NAO desenha ESP, caixa, nome, distancia ou direcao - so um aviso;</li>
 *     <li>somente dentro do raio configurado (4 blocos por padrao), ou seja,
 *         praticamente so quando o jogador encosta em voce e a mira dele
 *         deveria estar em voce;</li>
 *     <li><b>desligado por padrao</b>, porque em servidores competitive
 *         qualquer automatez que reaja a inimigo invisivel pode ser
 *         interpretado como vantagem. Use por conta e risco.</li>
 * </ul>
 */
public final class InvisibleWarning extends HudMod {

    private static final long SOUND_COOLDOWN = 3000L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private long lastSound;
    private String warning;
    private long warningUntil;

    public InvisibleWarning() {
        super("Invisible Warning", "Avisa quando ha jogador invisivel perto (somente aviso)",
                ModCategory.BEDWARS, 4, 120, Keyboard.KEY_I);
        values.set("radius", 4.0);
        values.set("sound", true);
        values.set("onlyBedWars", true);
    }

    @Override
    public void onTick() {
        if (!RenderHelper.inWorld()) {
            return;
        }
        double radius = values.getDouble("radius", 4.0d);
        Entity found = null;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer || entity.isDead) {
                continue;
            }
            if (!entity.isInvisible()) {
                continue;
            }
            if (mc.thePlayer.getDistanceToEntity(entity) <= radius) {
                found = entity;
                break;
            }
        }

        if (found == null) {
            warning = null;
            return;
        }

        warning = "JOGADOR INVISIVEL PERTO";
        warningUntil = System.currentTimeMillis() + 1500L;

        if (values.getBoolean("sound", true) && System.currentTimeMillis() - lastSound > SOUND_COOLDOWN) {
            lastSound = System.currentTimeMillis();
            mc.theWorld.playSound(mc.thePlayer, SoundEvents.BLOCK_NOTE_PLING, 1f, 0.6f);
        }
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (warning == null || System.currentTimeMillis() > warningUntil) {
            return;
        }
        SolarFont font = SolarFont.get("Sora", 14, true);
        String text = "! " + warning + " !";
        int width = (int) font.width(text) + 24;
        int x = (event.screenWidth - width) / 2;
        int y = 10;

        begin();
        RenderUtils.roundedRect(x, y, width, 24, 8f, Colors.argb(200, 90, 20, 30));
        RenderUtils.roundedOutline(x, y, width, 24, 8f, Colors.argb(150, 0xFF, 0x5C, 0x5C));
        font.drawCentered(text, x + width / 2f, y + 16, Colors.WHITE);
        setSize(width, 24);
        end();
    }
}
