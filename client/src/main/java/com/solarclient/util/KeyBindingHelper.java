package com.solarclient.util;

import net.minecraft.client.settings.KeyBinding;

/**
 * Ajuda a manipular {@link KeyBinding} do vanilla sem reflection.
 *
 * <p>{@code KeyBinding.setKeyBindState} e publico no 1.8.9, mas o Vanilla
 * guarda o estado por tecla em uma lista. Este metodo mantem os dois em
 * sincronia, o que evita o caso classico de "a tecla fica presa".</p>
 */
public final class KeyBindingHelper {

    private KeyBindingHelper() {
    }

    /** Marca a tecla como pressionada (ou solta). */
    public static void press(int keyCode, boolean pressed) {
        KeyBinding.setKeyBindState(keyCode, pressed);
    }

    public static void release(int keyCode) {
        KeyBinding.setKeyBindState(keyCode, false);
    }
}
