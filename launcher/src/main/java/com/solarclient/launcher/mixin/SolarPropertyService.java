package com.solarclient.launcher.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;
import java.util.Map;

/**
 * <h1>Servico de propriedades globais do Mixin</h1>
 *
 * <p>O Mixin guarda aqui as propriedades globais dele (por exemplo
 * {@code mixin.debug} e {@code mixin.env.remapRefMap}). Ele tambem nao traz
 * implementacao pronta: procura uma {@link IGlobalPropertyService} por
 * {@link java.util.ServiceLoader}, e no Forge essa implementacao vem do
 * LaunchWrapper.</p>
 *
 * <p>Como este projeto roda sem Forge, o proprio launcher fornece a dele — e
 * uma simples: um mapa de chaves para valores, que e tudo que o Mixin
 * precisa guardar.</p>
 *
 * <p>Vale notar o que este servico <b>nao</b> faz: ele nao le
 * {@code mixin.properties} e nao le variaveis de ambiente de proposito. As
 * opcoes do launcher (por exemplo {@code -Dmixin.debug=true}) chegam pelo
 * caminho normal do sistema, e o {@link MixinBootstrapper} nao depende delas
 * para funcionar.</p>
 *
 * @see SolarMixinService
 */
public final class SolarPropertyService implements IGlobalPropertyService {

    /** Nomes ja resolvidos: o Mixin pede a mesma chave mais de uma vez. */
    private final Map<String, Key> keys = new HashMap<>();

    /** Valores guardados, indexados pela chave ja resolvida. */
    private final Map<Key, Object> values = new HashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        Key key = keys.get(name);
        if (key == null) {
            key = new Key(name);
            keys.put(name, key);
        }
        return key;
    }

    @Override
    public <T> T getProperty(IPropertyKey key) {
        return getProperty(key, null);
    }

    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        if (!(key instanceof Key)) {
            return defaultValue;
        }
        Object value = values.get(key);
        // Sem valor ainda devolve o padrao do Mixin para aquela chave, e nao
        // null solto: assim o chamador nunca precisa tratar null.
        return value != null ? cast(value) : defaultFor(key, defaultValue);
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        if (!(key instanceof Key)) {
            return;
        }
        if (value == null) {
            values.remove(key);
        } else {
            values.put((Key) key, value);
        }
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        String value = getProperty(key, defaultValue);
        return value == null ? defaultValue : value.toString();
    }

    /**
     * Valor padrao de uma propriedade do Mixin.
     *
     * <p>O Mixin consulta algumas chaves com nome especifico (por exemplo as de
     * debug e de obfuscation). Devolvemos {@code "false"} para essas, que e o
     * comportamento normal em producao.</p>
     */
    @SuppressWarnings("unchecked")
    private <T> T defaultFor(IPropertyKey key, T defaultValue) {
        String name = key instanceof Key ? ((Key) key).name : "";
        if (defaultValue != null) {
            return defaultValue;
        }
        if (name.endsWith("debug") || name.endsWith("verbose")) {
            return (T) Boolean.FALSE;
        }
        if (name.endsWith("remapRefMap") || name.endsWith("refmapRemapping")) {
            return (T) Boolean.FALSE;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    /** Chave de propriedade: a identidade e o nome, como o Mixin espera. */
    private static final class Key implements IPropertyKey {
        private final String name;

        Key(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
