package com.solarclient.cosmetics;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.solarclient.util.ChatUtils;
import com.solarclient.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <h1>CosmeticManager</h1>
 *
 * <p>Centro do sistema de cosmeticos. Ele cuida de quatro coisas:</p>
 * <ol>
 *     <li><b>cache local</b> - o que esta equipado fica em
 *         {@code solar-client/cosmetics.json}, entao o client abre offline e
 *         ja mostra a capa certa;</li>
 *     <li><b>API</b> - a cada 5 minutos busca o que o jogador desbloqueou
 *         e salva o equipado no servidor (ver {@link CosmeticsApi});</li>
 *     <li><b>render</b> - desenha o cosmetico do jogador local e dos
 *         remotos (os remotos chegam pelo {@link com.solarclient.network.CosmeticNetwork});</li>
 *     <li><b>equipar</b> - usado pela GUI de cosmeticos.</li>
 * </ol>
 *
 * <p>Registro por jogador: {@code uuid -> (tipo -> cosmetico)}.</p>
 */
public final class CosmeticManager {

    private static final CosmeticManager INSTANCE = new CosmeticManager();

    private static final String FILE = "cosmetics.json";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Type MAP_TYPE = new TypeToken<Map<String, Map<String, String>>>() {
    }.getType();

    private final File file = new File("solar-client", FILE);
    private final Gson gson = new Gson();

    /** Catalogo de cosmeticos que existem no client. */
    private final Map<String, Cosmetic> catalog = new LinkedHashMap<>();
    /** uuid -> (tipo do cosmetico -> id) */
    private final Map<String, Map<CosmeticType, String>> equipped = new HashMap<>();
    /** Cache do que o jogador desbloqueou (id -> raridade). */
    private final Map<String, Rarity> owned = new HashMap<>();

    private final CosmeticsApi api = new CosmeticsApi();

    /** Acesso a API (usado pela GUI de cosmeticos e pela aba Settings). */
    public CosmeticsApi api() {
        return api;
    }
    private int syncCooldown;

    private CosmeticManager() {
        buildCatalog();
    }

    public static CosmeticManager get() {
        return INSTANCE;
    }

    /** Cria os cosmeticos "de fábrica" do client. */
    private void buildCatalog() {
        register(new AnimatedCape("solar_cape", Rarity.LEGENDARY, "Capa Solar"));
        register(new AnimatedCape("ember_cape", Rarity.EPIC, "Capa Brasa"));
        register(new AnimatedCape("frost_cape", Rarity.RARE, "Capa Gelo"));
        // Proximos cosmeticos (asas, chapeu, ...) entram aqui - veja o
        // comentario no final de AnimatedCape para o padrao de render.
    }

    public void register(Cosmetic cosmetic) {
        catalog.put(cosmetic.getId(), cosmetic);
    }

    // ------------------------------------------------------------------ cache
    public void loadLocal() {
        if (!file.exists()) {
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), UTF8)) {
            Map<String, Map<String, String>> data = gson.fromJson(reader, MAP_TYPE);
            if (data != null) {
                equipped.clear();
                for (Map.Entry<String, Map<String, String>> player : data.entrySet()) {
                    Map<CosmeticType, String> slots = new HashMap<>();
                    for (Map.Entry<String, String> slot : player.getValue().entrySet()) {
                        try {
                            slots.put(CosmeticType.valueOf(slot.getKey()), slot.getValue());
                        } catch (IllegalArgumentException ignored) {
                            // tipo desconhecido no cache: ignora
                        }
                    }
                    equipped.put(player.getKey(), slots);
                }
                Logger.info("Cosmeticos locais: " + equipped.size() + " jogador(es)");
            }
        } catch (Exception e) {
            Logger.error("Falha ao ler " + file + " (sera recriado)", e);
        }
    }

    public void saveLocal() {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                Logger.warn("Nao foi possivel criar a pasta " + dir);
                return;
            }
            Map<String, Map<String, String>> plain = new HashMap<>();
            for (Map.Entry<String, Map<CosmeticType, String>> player : equipped.entrySet()) {
                Map<String, String> slots = new HashMap<>();
                for (Map.Entry<CosmeticType, String> slot : player.getValue().entrySet()) {
                    slots.put(slot.getKey().name(), slot.getValue());
                }
                plain.put(player.getKey(), slots);
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), UTF8)) {
                gson.toJson(plain, writer);
            }
        } catch (Exception e) {
            Logger.error("Falha ao salvar " + file, e);
        }
    }

    // ------------------------------------------------------------------ API
    /** Sincroniza com a API a cada 5 minutos (na thread principal, e rapido). */
    public void onTick() {
        if (syncCooldown-- > 0) {
            return;
        }
        syncCooldown = 20 * 60 * 5;
        syncWithApi();
    }

    private void syncWithApi() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getSession() == null) {
            return;
        }
        UUID uuid = uuidOf(mc);
        if (uuid == null) {
            return;
        }
        // chamado fora da thread de render: o HttpClient do proprio modulo faz
        // a requisicao em uma thread daemon e devolve por callback
        api.fetchLoadout(uuid, loadout -> applyRemoteLoadout(uuid, loadout));
        api.saveLoadout(uuid, loadout(uuid), () -> {
            Logger.info("Loadout sincronizado com a API.");
        });
    }

    /** Aplica o que veio do servidor (usado pelo callback da API e pela rede). */
    public void applyRemoteLoadout(UUID uuid, Map<CosmeticType, String> loadout) {
        if (uuid == null || loadout == null) {
            return;
        }
        equipped.put(uuid.toString(), new HashMap<>(loadout));
        saveLocal();
    }

    public Map<CosmeticType, String> loadout(UUID uuid) {
        Map<CosmeticType, String> slots = equipped.get(String.valueOf(uuid));
        return slots == null ? new HashMap<CosmeticType, String>() : new HashMap<>(slots);
    }

    public void equip(UUID uuid, CosmeticType type, String cosmeticId) {
        Map<CosmeticType, String> slots = equipped.get(String.valueOf(uuid));
        if (slots == null) {
            slots = new HashMap<>();
            equipped.put(String.valueOf(uuid), slots);
        }
        if (cosmeticId == null) {
            slots.remove(type);
        } else {
            slots.put(type, cosmeticId);
        }
        saveLocal();
    }

    public List<Cosmetic> catalogOf(CosmeticType type) {
        List<Cosmetic> list = new ArrayList<>();
        for (Cosmetic cosmetic : catalog.values()) {
            if (cosmetic.getType() == type) {
                list.add(cosmetic);
            }
        }
        return list;
    }

    public Cosmetic byId(String id) {
        return catalog.get(id);
    }

    public Rarity rarityOf(String id) {
        Rarity ownedRarity = owned.get(id);
        return ownedRarity != null ? ownedRarity : Rarity.COMMON;
    }

    // ------------------------------------------------------------------ render
    /**
     * Desenha o cosmetico de um jogador (chamado pelo mixin do RenderPlayer).
     */
    public void renderFor(EntityPlayer player, float partialTicks) {
        if (player == null) {
            return;
        }
        Cosmetic cosmetic = resolve(player);
        if (cosmetic == null) {
            return;
        }
        try {
            cosmetic.render(player, partialTicks);
        } catch (Throwable t) {
            Logger.error("Erro ao desenhar o cosmetico " + cosmetic.getId(), t);
        }
    }

    /** O jogador tem uma capa customizada? (usado para esconder a do vanilla) */
    public boolean hasCustomCape(net.minecraft.entity.player.AbstractClientPlayer player) {
        return player != null && resolve(player) instanceof AnimatedCape;
    }

    private Cosmetic resolve(EntityPlayer player) {
        String key = player.getUniqueID().toString();
        // primeiro tenta pelo UUID de sessao (jogadores sem conta known)
        Map<CosmeticType, String> slots = equipped.get(key);
        if (slots == null) {
            slots = equipped.get(player.getName());
        }
        if (slots == null) {
            return null;
        }
        String capeId = slots.get(CosmeticType.CAPE);
        return capeId == null ? null : catalog.get(capeId);
    }

    /** Reservado: hoje nenhum cosmetico desenha no HUD. */
    public void onRender2D(com.solarclient.event.Events.Render2D event) {
    }

    /** Reservado: hoje nenhum cosmetico desenha no HUD. */
    public void onRender3D(com.solarclient.event.Events.Render3D event) {
    }

    /** Limpa o cache quando o mundo e descarregado. */
    public void clearRemote() {
        // o cache local continua valendo; so zera a fila de sincronizacao
        syncCooldown = 0;
    }

    // ------------------------------------------------------------------ util
    public static UUID uuidOf(Minecraft mc) {
        if (mc == null || mc.getSession() == null) {
            return null;
        }
        try {
            return UUID.fromString(mc.getSession().getPlayerID().replace("-", ""));
        } catch (Exception e) {
            return null;
        }
    }

    /** Nome "limpo" do jogador (usado nas mensagens de log). */
    public static String nameOf(EntityPlayer player) {
        return player == null ? "?" : ChatUtils.strip(player.getName());
    }
}
