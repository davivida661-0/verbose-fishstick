package com.solarclient.cosmetics;

/**
 * Tipos de cosmetico suportados. O valor do enum e o que vai no JSON e na
 * API, entao <b>nao mude o texto</b> depois de publicar (use o id numerico do
 * banco se precisar versionar).
 */
public enum CosmeticType {

    CAPE("Capa", true),
    CLOAK("Manto", true),
    DRAGON_WINGS("Asas de Dragao", false),
    ANGEL_WINGS("Asas de Anjo", false),
    BANDANA("Bandana", false),
    HAT("Chapeu", false),
    GLASSES("Oculos", false);

    private final String displayName;
    /** Se {@code true}, o item ocupa as costas (capa/manto). */
    private final boolean back;

    CosmeticType(String displayName, boolean back) {
        this.displayName = displayName;
        this.back = back;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBack() {
        return back;
    }
}
