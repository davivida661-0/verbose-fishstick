package com.solarclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;

/**
 * Todos os eventos do Solar Client.
 *
 * <p>Estao agrupados em uma unica classe de proposito: no Java 8 nao existem
 * arquivos com multiplas classes publicas, e assim o codigo fica curto de ler
 * ({@code Events.Tick}, {@code Events.Render2D}, ...).</p>
 */
public final class Events {

    private Events() {
    }

    /** Chamado 20x por segundo dentro do loop do jogo. */
    public static final class Tick extends Event {
        /** Instancia unica: o evento nao guarda estado. */
        public static final Tick INSTANCE = new Tick();
        /** Contador global de ticks (1 hora = 72.000). */
        public int tickCount;

        private Tick() {
        }
    }

    /** Camada 2D, desenhada por cima do HUD vanilla. Coordenadas ja escaladas. */
    public static final class Render2D extends Event {
        public int screenWidth;
        public int screenHeight;
        public int mouseX;
        public int mouseY;
        public int tickCounter;
        public float partialTicks;
        public final Minecraft mc = Minecraft.getMinecraft();

        public Render2D(int width, int height, int mouseX, int mouseY, float partialTicks) {
            this.screenWidth = width;
            this.screenHeight = height;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.partialTicks = partialTicks;
        }
    }

    /** Camada 3D (mundo), util para NameTags, cosmeticos e ESP de alerta. */
    public static final class Render3D extends Event {
        public float partialTicks;
        public final Minecraft mc = Minecraft.getMinecraft();

        public Render3D(float partialTicks) {
            this.partialTicks = partialTicks;
        }
    }

    /** Tecla pressionada ou solta. */
    public static final class Key extends Event {
        public final int keyCode;
        public final boolean pressed;

        public Key(int keyCode, boolean pressed) {
            this.keyCode = keyCode;
            this.pressed = pressed;
        }
    }

    /** Mensagem que o jogador esta enviando no chat. */
    public static final class ChatOutgoing extends Event {
        public String message;

        public ChatOutgoing(String message) {
            this.message = message;
        }
    }

    /**
     * Mensagem que chegou do servidor (sem codigos de cor).
     *
     * <p>Se algum mod preencher {@link #replacement} e cancelar o evento, o
     * mixin imprime o componente novo no lugar do original. E assim que o
     * Nick Hider reescreve o seu nome no chat.</p>
     */
    public static final class ChatIncoming extends Event {
        public final String plain;
        public IChatComponent replacement;

        public ChatIncoming(String plain) {
            this.plain = plain;
        }
    }

    /** Payload customizado: e assim que os cosmeticos viajam entre jogadores. */
    public static final class CustomPayload extends Event {
        public final String channel;
        public final byte[] data;

        public CustomPayload(String channel, byte[] data) {
            this.channel = channel;
            this.data = data;
        }
    }
}
