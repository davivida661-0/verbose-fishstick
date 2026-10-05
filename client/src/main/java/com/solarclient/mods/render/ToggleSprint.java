package com.solarclient.mods.render;

import com.solarclient.mod.Mod;
import com.solarclient.mod.ModCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.lwjgl.input.Keyboard;

/**
 * ToggleSprint.
 *
 * <p>Corrida continua sem segurar Ctrl: da dois toques em "frente" (W) e o
 * jogador fica correndo ate parar de andar. E o mesmo gesto do proprio
 * Minecraft (duplo toque existe desde o 1.6) - so que aqui ele vira um toggle
 * explicito e mais tolerante.</p>
 *
 * <p>Importante para anticheat: o estado de sprint e um campo normal do
 * jogador e viaja no pacote de movimento. Nao ha pacote extra, entao o
 * Watchdog do Hypixel ve exatamente o que veria sem o mod.</p>
 */
public final class ToggleSprint extends Mod {

    private static final long DOUBLE_TAP_MS = 280L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasForward;
    private long lastTap;
    private boolean sprinting;

    public ToggleSprint() {
        super("ToggleSprint", "Corra com dois toques em W, sem segurar Ctrl",
                ModCategory.RENDER, Keyboard.KEY_Z);
        values.set("requireMovement", true);
    }

    @Override
    public void onTick() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) {
            return;
        }
        boolean forward = mc.gameSettings.keyBindForward.isKeyDown();

        // Detecta o duplo toque: duas descidas de "solto" -> "pressionado"
        // em menos de 280ms.
        if (forward && !wasForward) {
            long now = System.currentTimeMillis();
            if (now - lastTap < DOUBLE_TAP_MS) {
                sprinting = !sprinting;
                lastTap = 0;
            } else {
                lastTap = now;
            }
        }
        if (!forward) {
            sprinting = false;
            lastTap = 0;
        }
        wasForward = forward;

        boolean wantSprint = sprinting && (!values.getBoolean("requireMovement", true) || forward);
        if (wantSprint) {
            player.setSprinting(true);
        } else if (!mc.gameSettings.keyBindSprint.isKeyDown()) {
            player.setSprinting(false);
        }
    }
}
