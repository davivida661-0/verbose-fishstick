package com.solarclient.mod;

import com.solarclient.event.Events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>ModManager</h1>
 *
 * <p>Registro central de todos os modulos. Cuida de:</p>
 * <ul>
 *     <li>registrar/buscar mods por classe ou por nome;</li>
 *     <li>ligar as KeyBinding e alternar o mod quando a tecla e pressionada;</li>
 *     <li>repassar os hooks de tick/render apenas para os mods LIGADOS
 *         (mod desligado custa literalmente zero no loop);</li>
 *     <li>guardar a lista de mods de HUD que o HUD Editor manipula.</li>
 * </ul>
 */
public final class ModManager {

    /** Ordem de insercao = ordem em que aparecem no ClickGUI. */
    private final Map<String, Mod> mods = new LinkedHashMap<>();
    private final List<HudMod> hudMods = new ArrayList<>();

    // ------------------------------------------------------------------ registro
    public void register(Mod mod) {
        if (mods.containsKey(mod.getName())) {
            throw new IllegalStateException("Mod duplicado: " + mod.getName());
        }
        mods.put(mod.getName(), mod);
        if (mod instanceof HudMod) {
            hudMods.add((HudMod) mod);
        }
    }

    /** Cria todas as KeyBinding (chamar depois do config ser aplicado). */
    public void linkKeyBindings() {
        for (Mod mod : mods.values()) {
            mod.linkKeyBinding();
        }
    }

    // ------------------------------------------------------------------ buscas
    public <T extends Mod> T get(Class<T> type) {
        for (Mod mod : mods.values()) {
            if (type.isInstance(mod)) {
                return type.cast(mod);
            }
        }
        return null;
    }

    public Mod byName(String name) {
        return mods.get(name);
    }

    public Collection<Mod> all() {
        return Collections.unmodifiableCollection(mods.values());
    }

    public List<Mod> byCategory(ModCategory category) {
        List<Mod> list = new ArrayList<>();
        for (Mod mod : mods.values()) {
            if (mod.getCategory() == category) {
                list.add(mod);
            }
        }
        return list;
    }

    public List<HudMod> hudMods() {
        return Collections.unmodifiableList(hudMods);
    }

    public int size() {
        return mods.size();
    }

    // ------------------------------------------------------------------ hooks
    public void onTick() {
        for (Mod mod : mods.values()) {
            // Tecla: o vanilla chama KeyBinding.onTick no evento de teclado,
            // entao isPressed() aqui funciona igual a um toggle normal.
            if (mod.getKeyBinding() != null && mod.getKeyBinding().isPressed()) {
                mod.toggle();
            }
            if (mod.isEnabled()) {
                mod.onTick();
            }
        }
    }

    public void onRender2D(Events.Render2D event) {
        for (Mod mod : mods.values()) {
            if (mod.isEnabled()) {
                mod.onRender2D(event);
            }
        }
    }

    public void onRender3D(Events.Render3D event) {
        for (Mod mod : mods.values()) {
            if (mod.isEnabled()) {
                mod.onRender3D(event);
            }
        }
    }

    // ------------------------------------------------------------------ util
    /** Liga/desliga e devolve o novo estado (usado no ClickGUI). */
    public boolean toggle(Mod mod) {
        mod.toggle();
        return mod.isEnabled();
    }

    /** Quantos mods estao ligados (mostrado na tela de statut). */
    public int enabledCount() {
        int n = 0;
        for (Mod mod : mods.values()) {
            if (mod.isEnabled()) {
                n++;
            }
        }
        return n;
    }
}
