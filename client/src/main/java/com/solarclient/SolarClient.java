package com.solarclient;

import com.solarclient.config.ConfigManager;
import com.solarclient.cosmetics.CosmeticManager;
import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import com.solarclient.mod.ModManager;
import com.solarclient.mod.ModRegistry;
import com.solarclient.util.Logger;
import net.minecraft.util.IChatComponent;

/**
 * <h1>Classe principal do Solar Client</h1>
 *
 * <p>Ponto unico de inicializacao do client. Ela e chamada pelo launcher
 * ({@code com.solarclient.launcher.SolarGameLauncher}) logo apos o
 * Minecraft 1.8.9 criar a janela e o singleton {@code Minecraft}.</p>
 *
 * <p>Responsabilidades:</p>
 * <ol>
 *     <li>carregar o config JSON ({@link ConfigManager});</li>
 *     <li>registrar todos os mods ({@link ModRegistry});</li>
 *     <li>ligar as KeyBindings e aplicar o estado salvo;</li>
 *     <li>iniciar o barramento de eventos e o gerenciador de cosmeticos.</li>
 * </ol>
 *
 * <p>A partir dai e so forwarding: cada mixin posta um evento no
 * {@link EventBus} e o {@link ModManager} repassa para os mods ligados.</p>
 */
public final class SolarClient {

    // ---------------------------------------------------------------- identidade
    public static final String NAME = "Solar Client";
    public static final String SHORT_NAME = "Solar";
    public static final String VERSION = "0.1.0";
    public static final String MC_VERSION = "1.8.9";
    public static final String MOD_ID = "solarclient";

    private static final SolarClient INSTANCE = new SolarClient();

    // ---------------------------------------------------------------- nucleo
    public final EventBus eventBus = new EventBus();
    public final ModManager modManager = new ModManager();
    public final ConfigManager configManager = new ConfigManager();
    public final CosmeticManager cosmeticManager = CosmeticManager.get();

    private boolean initialized;

    private SolarClient() {
    }

    public static SolarClient get() {
        return INSTANCE;
    }

    public static SolarClient instance() {
        return INSTANCE;
    }

    public static boolean isInitialized() {
        return INSTANCE.initialized;
    }

    // ---------------------------------------------------------------- init
    /**
     * Inicializa o client. E seguro chamar varias vezes (o launcher pode chamar
     * de novo se o usuario voltar para a tela inicial).
     */
    public void init() {
        if (initialized) {
            return;
        }

        Logger.info("Iniciando " + NAME + " v" + VERSION + " para Minecraft " + MC_VERSION);

        // 1) config do usuario (tema, mods, posicoes do HUD, valores de cada mod)
        configManager.load();

        // 2) cria e registra todos os mods do catalogo
        ModRegistry.registerAll(modManager);

        // 3) aplica enabled / x / y / scale / valores salvos em cada mod
        configManager.applyToMods(modManager);

        // 4) cria as KeyBinding do vanilla (1.8.9 registra sozinha no GameSettings)
        modManager.linkKeyBindings();

        // 4b) mods podem declarar @EventBus.Handler e receber eventos sozinhos
        for (com.solarclient.mod.Mod mod : modManager.all()) {
            eventBus.register(mod);
        }
        com.solarclient.util.CpsTracker.register(eventBus);
        com.solarclient.bedwars.SessionTracker.register(eventBus);
        com.solarclient.bedwars.BedStatusTracker.register(eventBus);
        com.solarclient.network.CosmeticNetwork.register(eventBus);
        com.solarclient.gui.GuiShortcuts.register(eventBus);

        // 5) cosmeticos: cache local primeiro, sincroniza com a API depois
        cosmeticManager.loadLocal();

        initialized = true;
        Logger.info(NAME + " pronto: " + modManager.size() + " mods carregados.");
    }

    /** Salva tudo e desliga de forma limpa. */
    public void shutdown() {
        if (!initialized) {
            return;
        }
        configManager.save(modManager);
        cosmeticManager.saveLocal();
        initialized = false;
        Logger.info("Shutdown limpo, configuracao salva.");
    }

    // ---------------------------------------------------------------- forwarding
    /** Chamado uma vez por tick (20x/segundo) pelo mixin do Minecraft. */
    public void onTick() {
        Events.Tick tick = Events.Tick.INSTANCE;
        tick.tickCount++;
        eventBus.post(tick);
        modManager.onTick();
        cosmeticManager.onTick();
    }

    /** Desenhado por cima do HUD do jogo (mixin do GuiIngame). */
    public void onRender2D(Events.Render2D event) {
        eventBus.post(event);
        modManager.onRender2D(event);
        cosmeticManager.onRender2D(event);
    }

    /** Roda durante a renderizacao do mundo 3D (mixin do EntityRenderer). */
    public void onRender3D(Events.Render3D event) {
        eventBus.post(event);
        modManager.onRender3D(event);
        cosmeticManager.onRender3D(event);
    }

    /** Mensagem que o jogador esta enviando para o servidor. */
    public void onChatOutgoing(String message) {
        Events.ChatOutgoing event = new Events.ChatOutgoing(message);
        eventBus.post(event);
    }

    /**
     * Mensagem que chegou do servidor (chat, death screen, title, etc).
     *
     * <p>Recebe o componente original do protocolo: um mod pode devolver um
     * componente novo (Nick Hider) ou cancelar a mensagem. O mixin imprime o
     * que for devolvido.</p>
     *
     * @return o componente a ser exibido (nunca null se a mensagem nao foi cancelada)
     */
    public IChatComponent onChatIncoming(IChatComponent component) {
        Events.ChatIncoming event = new Events.ChatIncoming(
                com.solarclient.util.ChatUtils.getUnformattedText(component));
        eventBus.post(event);
        if (event.isCancelled()) {
            return event.replacement;
        }
        return component;
    }

    /** Payload customizado (canal "solar|cosmetics") vindo do servidor. */
    public void onCustomPayload(String channel, byte[] data) {
        eventBus.post(new Events.CustomPayload(channel, data));
    }

    /** Tecla pressionada (usado pelo ChatMod / AutoGG). */
    public void onKey(int keyCode, boolean pressed) {
        eventBus.post(new Events.Key(keyCode, pressed));
    }
}
