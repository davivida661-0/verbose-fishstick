package com.solarclient.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

/**
 * Utilitarios de texto: remover codigos de cor (§), encurtar nomes e extrair
 * texto de um {@link IChatComponent} do protocolo.
 */
public final class ChatUtils {

    /** Codigo de cor do Minecraft: § seguido de um digito/letra. */
    private static final char FORMATTING = '§';
    /** Mesma coisa, mas na codificacao legada (0xA7) que alguns servidores usam. */
    private static final char SECTION = 0xA7;

    private ChatUtils() {
    }

    /** Remove todos os codigos §x do texto. */
    public static String strip(String text) {
        if (text == null || text.indexOf(FORMATTING) < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == FORMATTING || c == SECTION) {
                i++; // pula o codigo
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    /** Sobra do texto depois de um {@code :} (usado para limpar kill feed). */
    public static String afterColon(String text) {
        String clean = strip(text);
        int index = clean.indexOf(": ");
        return index >= 0 ? clean.substring(index + 2) : clean;
    }

    public static String getUnformattedText(IChatComponent component) {
        return component == null ? "" : strip(component.getUnformattedText());
    }

    public static String getFormattedText(IChatComponent component) {
        return component == null ? "" : component.getFormattedText();
    }

    /**
     * Nome curto do item para caber em um quadradinho de 16x16 do HUD.
     * Ex.: "diamond_sword" -&gt; "DIAM".
     */
    public static String shortenItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String name = stack.getUnlocalizedName();
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(colon + 1);
        }
        name = name.replace('_', ' ').trim();
        if (name.isEmpty()) {
            return "";
        }
        String firstWord = name.split(" ")[0];
        String upper = firstWord.length() > 5
                ? firstWord.substring(0, 4)
                : firstWord;
        return upper.toUpperCase();
    }

    /** "12.3s" a partir de milissegundos. */
    public static String formatMillis(long millis) {
        return String.format("%.1fs", millis / 1000f);
    }
}
