package com.solarclient.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solarclient.cosmetics.CosmeticManager;
import com.solarclient.cosmetics.CosmeticType;
import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C17PacketCustomPayload;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <h1>Sincronia de cosmeticos entre jogadores</h2>
 *
 * <p>Para a capa de um jogador aparecer para os <b>outros</b> jogadores que
 * usam o Solar Client, o cliente manda um payload customizado (o mesmo
 * mecanismo de mod do 1.8.9) para o servidor, que repassa para todos.</p>
 *
 * <p><b>Importante:</b> o servidorHypixel (e qualquer outro) ignora canais
 * desconhecidos - o pacote nao atrapalha nada e o modulo simplesmente nunca
 * roda la. Quem tem o client ve o cosmetico; quem nao tem, ve o player normal.
 * Nenhum dado de gameplay trafega nesse canal.</p>
 *
 * <p>Formato do payload (JSON UTF-8, canal {@value #CHANNEL}):</p>
 * <pre>
 *   { "uuid": "uuid-sem-traco", "slots": { "CAPE": "solar_cape" } }
 * </pre>
 */
public final class CosmeticNetwork {

    public static final String CHANNEL = "solar|cosmetics";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_BYTES = 32 * 1024;

    private static final CosmeticNetwork INSTANCE = new CosmeticNetwork();

    private final Gson gson = new Gson();
    private final CosmeticManager manager = CosmeticManager.get();

    private CosmeticNetwork() {
    }

    public static CosmeticNetwork get() {
        return INSTANCE;
    }

    /** Registra o listener (chamado no {@code SolarClient#init}). */
    public static void register(EventBus bus) {
        bus.register(new Listener());
    }

    /** Esse payload e nosso? (o mixin cancela so quando true) */
    public static boolean isSolarChannel(C17PacketCustomPayload packet) {
        return packet != null && CHANNEL.equals(packet.getChannelName());
    }

    // ------------------------------------------------------------------ envio
    /** Manda o equipamento atual para o servidor. */
    public void broadcast() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }
        UUID uuid = CosmeticManager.uuidOf(mc);
        if (uuid == null) {
            return;
        }
        byte[] data = encode(uuid, manager.loadout(uuid));
        if (data == null) {
            return;
        }
        try {
            mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload(CHANNEL, data));
            Logger.info("Loadout de cosmeticos enviado (" + data.length + " bytes).");
        } catch (Exception e) {
            Logger.warn("Nao foi possivel enviar o loadout: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ leitura
    private byte[] encode(UUID uuid, Map<CosmeticType, String> slots) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        JsonObject jsonSlots = new JsonObject();
        for (Map.Entry<CosmeticType, String> slot : slots.entrySet()) {
            jsonSlots.addProperty(slot.getKey().name(), slot.getValue());
        }
        json.add("slots", jsonSlots);
        byte[] data = gson.toJson(json).getBytes(UTF8);
        return data.length <= MAX_BYTES ? data : null;
    }

    public void handle(C17PacketCustomPayload packet) {
        try {
            String text = new String(packet.getBuffer(), UTF8);
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            UUID uuid = UUID.fromString(json.get("uuid").getAsString());
            JsonObject slots = json.getAsJsonObject("slots");

            Map<CosmeticType, String> loadout = new HashMap<>();
            for (String key : slots.keySet()) {
                try {
                    loadout.put(CosmeticType.valueOf(key), slots.get(key).getAsString());
                } catch (IllegalArgumentException ignored) {
                    // tipo desconhecido: ignora
                }
            }
            manager.applyRemoteLoadout(uuid, loadout);
        } catch (Exception e) {
            Logger.warn("Payload de cosmetico invalido: " + e.getMessage());
        }
    }

    /** Reenvia o loadout quando o jogador entra em um mundo. */
    private static final class Listener {
        @EventBus.Handler
        public void onTick(Events.Tick event) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null) {
                return;
            }
            if (lastWorld != mc.theWorld) {
                lastWorld = mc.theWorld;
                INSTANCE.broadcast();
            }
        }

        private net.minecraft.client.multiplayer.WorldClient lastWorld;
    }
}
