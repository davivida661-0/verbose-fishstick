package com.solarclient.mod;

import com.solarclient.bedwars.AutoGG;
import com.solarclient.bedwars.AutoPlayAgain;
import com.solarclient.bedwars.AutoTip;
import com.solarclient.bedwars.BedWarsOverlay;
import com.solarclient.bedwars.FireballJump;
import com.solarclient.bedwars.InvisibleWarning;
import com.solarclient.bedwars.LowHealth;
import com.solarclient.bedwars.NickHider;
import com.solarclient.bedwars.SessionStats;
import com.solarclient.mods.performance.Performance;
import com.solarclient.mods.pvp.ArmorStatus;
import com.solarclient.mods.pvp.ChatMod;
import com.solarclient.mods.pvp.Coordinates;
import com.solarclient.mods.pvp.CpsCounter;
import com.solarclient.mods.pvp.Direction;
import com.solarclient.mods.pvp.FpsCounter;
import com.solarclient.mods.pvp.ItemInfo;
import com.solarclient.mods.pvp.Keystrokes;
import com.solarclient.mods.pvp.PackDisplay;
import com.solarclient.mods.pvp.PingCounter;
import com.solarclient.mods.pvp.PotionStatus;
import com.solarclient.mods.pvp.TabList;
import com.solarclient.mods.pvp.TimeMod;
import com.solarclient.mods.render.BossBar;
import com.solarclient.mods.render.ClearGlass;
import com.solarclient.mods.render.FullBright;
import com.solarclient.mods.render.HitColor;
import com.solarclient.mods.render.NameTags;
import com.solarclient.mods.render.NoHurtCam;
import com.solarclient.mods.render.OneSevenAnimations;
import com.solarclient.mods.render.ScoreboardMod;
import com.solarclient.mods.render.ToggleSneak;
import com.solarclient.mods.render.ToggleSprint;

/**
 * <h1>Registro de mods</h1>
 *
 * <p>Lista unica de tudo que existe no client. E aqui que voce adiciona um mod
 * novo: escreva a classe, depois acrescente uma linha nesta lista.</p>
 *
 * <p><b>Politica do projeto:</b> so entram mods visuais, de HUD, de QoL ou de
 * performance. Nenhum mod toca em pacote de movimento, hitbox ou dano - por
 * isso o client funciona em servidores com anticheat (Hypixel Watchdog,
 * Vulcan, Grim, etc.).</p>
 */
public final class ModRegistry {

    private ModRegistry() {
    }

    /** Registra todos os mods, na ordem em que aparecem no ClickGUI. */
    public static void registerAll(ModManager manager) {
        // ------------------------------------------------------------ PvP / HUD
        manager.register(new Keystrokes());
        manager.register(new CpsCounter());
        manager.register(new FpsCounter());
        manager.register(new PingCounter());
        manager.register(new ArmorStatus());
        manager.register(new PotionStatus());
        manager.register(new ItemInfo());
        manager.register(new PackDisplay());
        manager.register(new TabList());
        manager.register(new ScoreboardMod());
        manager.register(new BossBar());
        manager.register(new ChatMod());

        // ----------------------------------------------------------- Render
        manager.register(new OneSevenAnimations());
        manager.register(new NameTags());
        manager.register(new ToggleSprint());
        manager.register(new ToggleSneak());
        manager.register(new FullBright());
        manager.register(new ClearGlass());
        manager.register(new NoHurtCam());
        manager.register(new HitColor());
        manager.register(new Coordinates());
        manager.register(new Direction());
        manager.register(new TimeMod());
        manager.register(new Performance());

        // --------------------------------------------------------- BedWars
        manager.register(new BedWarsOverlay());
        manager.register(new SessionStats());
        manager.register(new FireballJump());
        manager.register(new LowHealth());
        manager.register(new NickHider());
        manager.register(new InvisibleWarning());
        manager.register(new AutoGG());
        manager.register(new AutoPlayAgain());
        manager.register(new AutoTip());
    }
}
