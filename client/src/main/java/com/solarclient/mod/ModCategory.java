package com.solarclient.mod;

/**
 * Categorias exibidas no ClickGUI.
 *
 * <p>As categorias sao fixas (naochem) para o usuario nunca ficar sem menu:
 * e cada mod declara em qual categoria ele aparece.</p>
 */
public enum ModCategory {

    COMBAT("PvP"),
    RENDER("Render"),
    BEDWARS("BedWars"),
    COSMETICS("Cosmetics"),
    SETTINGS("Settings");

    private final String displayName;

    ModCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
