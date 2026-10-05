package com.solarclient.mods.pvp;

import com.solarclient.event.Events;
import com.solarclient.mod.HudMod;
import com.solarclient.mod.ModCategory;
import com.solarclient.ui.SolarFont;
import com.solarclient.ui.Theme;
import com.solarclient.util.Reflect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePack;
import org.lwjgl.input.Keyboard;

/**
 * Pack Display.
 *
 * <p>Mostra o resource pack que o servidor mandou (nome) e a "client brand"
 * que o proprio cliente 1.8.9 envia (util para saber se o servidor te
 * identificou). Serve principalmente para descobrir por que o jogo ficou
 * cinza depois de entrar em um servidor.</p>
 *
 * <p>Os getters sao resolvidos por {@link Reflect} porque o nome do metodo do
 * gerenciador de packs mudou entre versoes do 1.8 - assim o mod funciona sem
 * precisar acertar o nome exato.</p>
 */
public final class PackDisplay extends HudMod {

    private final Minecraft mc = Minecraft.getMinecraft();
    private String packName = "-";
    private boolean hasPack;
    private String brand = "vanilla";

    public PackDisplay() {
        super("Pack Display", "Resource pack do servidor e client brand",
                ModCategory.RENDER, 4, 60, Keyboard.KEY_O);
        values.set("showBrand", true);
    }

    @Override
    public void onTick() {
        if (mc.theWorld == null) {
            hasPack = false;
            packName = "-";
            return;
        }
        hasPack = false;
        packName = "-";
        try {
            ResourcePack pack = Reflect.callGetter(mc.getResourcePackManager(), ResourcePack.class);
            if (pack != null) {
                hasPack = true;
                String name = Reflect.callGetter(pack, String.class);
                packName = name == null ? "custom" : shorten(name);
            }
        } catch (Exception e) {
            // servidor sem pack: mantem o "-"
        }
        if (values.getBoolean("showBrand", true) && mc.thePlayer != null) {
            String clientBrand = mc.thePlayer.getClientBrand();
            brand = clientBrand == null || clientBrand.isEmpty() ? "vanilla" : clientBrand;
        }
    }

    @Override
    public void onRender2D(Events.Render2D event) {
        if (mc.theWorld == null) {
            return;
        }
        int w = 150;
        int h = values.getBoolean("showBrand", true) ? 34 : 22;

        begin();
        drawPanel(w + 8, h + 6, 0.45f, 6f);

        SolarFont font = SolarFont.get();
        font.draw("pack:", 6, 15, Theme.TEXT_SECONDARY);
        font.draw(packName, 34, 15, hasPack ? Theme.getAccent() : Theme.TEXT_SECONDARY);

        if (values.getBoolean("showBrand", true)) {
            font.draw("brand:", 6, 27, Theme.TEXT_SECONDARY);
            font.draw(brand, 40, 27, "vanilla".equals(brand) ? Theme.TEXT_SECONDARY : Theme.SUCCESS);
        }

        setSize(w + 8, h + 6);
        end();
    }

    private static String shorten(String text) {
        return text.length() <= 12 ? text : text.substring(0, 11) + "…";
    }
}
