package com.solarclient.gui;

import com.solarclient.SolarClient;
import com.solarclient.config.ConfigManager;
import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/**
 * <h1>Atalhos das telas do client</h1>
 *
 * <p>Abre as telas com <b>combos de teclas</b> (o padrao dos clients grandes,
 * tipo o "RS" do Badlion) em vez de uma tecla solta - assim nao briga com os
 * atalhos dos mods nem com os do jogo:</p>
 *
 * <pre>
 *   R + S   -&gt; ClickGUI
 *   H + U   -&gt; HUD Editor
 *   C + O   -&gt; GUI de cosmeticos
 * </pre>
 *
 * <p><b>Funciona em qualquer tela</b>, inclusive no menu principal: a
 * deteccao acontece no tick e nao depende de GUI aberta, entao da para abrir
 * o client antes mesmo de entrar em um mundo.</p>
 *
 * <p>As teclas sao lidas com {@link Keyboard#isKeyDown(int)} (estado fisico),
 * e nao com o {@code KeyBinding} dos mods - sao dois sistemas diferentes de
 proposito. Os combos ficam salvos em {@code solar-client/config.json}, na
 * secao {@code gui}.</p>
 */
public final class GuiShortcuts {

    /** Combos padrao. */
    private static final int[] DEFAULT_CLICK_GUI = {Keyboard.KEY_R, Keyboard.KEY_S};
    private static final int[] DEFAULT_HUD_EDITOR = {Keyboard.KEY_H, Keyboard.KEY_U};
    private static final int[] DEFAULT_COSMETICS = {Keyboard.KEY_C, Keyboard.KEY_O};

    private GuiShortcuts() {
    }

    public static void register(EventBus bus) {
        bus.register(new Listener());
    }

    private static final class Listener {

        private final boolean[] click = {false};
        private final boolean[] hud = {false};
        private final boolean[] cosmetics = {false};

        @EventBus.Handler
        public void onTick(Events.Tick event) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                return;
            }

            if (chord(combo("clickGuiKey", DEFAULT_CLICK_GUI), click)) {
                open(mc, new ClickGUI());
            } else if (chord(combo("hudEditorKey", DEFAULT_HUD_EDITOR), hud)) {
                // o HUD Editor so faz sentido com o jogo rodando
                if (mc.thePlayer != null) {
                    open(mc, new HudEditorGui());
                }
            } else if (chord(combo("cosmeticsKey", DEFAULT_COSMETICS), cosmetics)) {
                open(mc, new CosmeticsGui());
            }
        }

        /**
         * Borda de subida do combo: as duas teclas estavam soltas e as duas
         * foram pressionadas. Soltar qualquer uma rearma.
         */
        private static boolean chord(int[] keys, boolean[] previous) {
            boolean allDown = true;
            for (int key : keys) {
                if (!Keyboard.isKeyDown(key)) {
                    allDown = false;
                    break;
                }
            }
            boolean pressed = allDown && !previous[0];
            previous[0] = allDown;
            return pressed;
        }

        private static void open(Minecraft mc, GuiScreen screen) {
            // troca de tela sem empilhar: apertar duas vezes nao abre duas GUIs
            mc.displayGuiScreen(screen);
            Logger.debug("Tela aberta pelo atalho: " + screen.getClass().getSimpleName());
        }
    }

    /** Le o combo salvo no config (ou usa o padrao). */
    private static int[] combo(String key, int[] fallback) {
        ConfigManager config = SolarClient.get().configManager;
        int[] configured = config.getChord(key);
        return configured == null || configured.length != fallback.length
                ? fallback
                : configured;
    }
}
