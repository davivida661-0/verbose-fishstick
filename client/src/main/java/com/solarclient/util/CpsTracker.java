package com.solarclient.util;

import com.solarclient.event.EventBus;
import com.solarclient.event.Events;
import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * <h1>Contador de CPS</h1>
 *
 * <p>Mede os cliques do mouse e guarda o historico do ultimo segundo. E
 * compartilhado: o mod Keystrokes usa para o brilho do botao e o mod CPS
 * Counter mostra o numero.</p>
 *
 * <p>Por que escutar o {@code Render2D} e nao o {@code Tick}? Porque o jogo
 * roda a 20 ticks/segundo, mas desenha a 100-240 FPS. Um clica rapido de 15 CPS
 * daria 2 cliques no mesmo tick e o contador marcaria 10. Escutando a cada
 * frame, a contagem fica correta.</p>
 */
public final class CpsTracker {

    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    private static boolean leftDown;
    private static boolean rightDown;
    private static long lastLeftPress;
    private static long lastRightPress;

    private CpsTracker() {
    }

    /** Registra o tracker no barramento de eventos (chamado no SolarClient#init). */
    public static void register(EventBus bus) {
        bus.register(new Listener());
    }

    public static int left() {
        prune(LEFT);
        return LEFT.size();
    }

    public static int right() {
        prune(RIGHT);
        return RIGHT.size();
    }

    /** Milissegundos desde o ultimo clique esquerdo (0 se nunca clicou). */
    public static long lastLeftPressAgo() {
        return lastLeftPress == 0 ? 0 : System.currentTimeMillis() - lastLeftPress;
    }

    public static long lastRightPressAgo() {
        return lastRightPress == 0 ? 0 : System.currentTimeMillis() - lastRightPress;
    }

    public static boolean isLeftDown() {
        return leftDown;
    }

    public static boolean isRightDown() {
        return rightDown;
    }

    public static void reset() {
        LEFT.clear();
        RIGHT.clear();
        leftDown = false;
        rightDown = false;
    }

    // ------------------------------------------------------------------ interno
    private static void poll() {
        long now = System.currentTimeMillis();

        boolean nowLeft = Mouse.isButtonDown(0);
        if (nowLeft && !leftDown) {
            LEFT.addLast(now);
            lastLeftPress = now;
        }
        leftDown = nowLeft;

        boolean nowRight = Mouse.isButtonDown(1);
        if (nowRight && !rightDown) {
            RIGHT.addLast(now);
            lastRightPress = now;
        }
        rightDown = nowRight;

        prune(LEFT);
        prune(RIGHT);
    }

    private static void prune(Deque<Long> deque) {
        long limit = System.currentTimeMillis() - 1000L;
        while (!deque.isEmpty() && deque.peekFirst() < limit) {
            deque.removeFirst();
        }
    }

    /** Objeto leve registrado no bus (o tracker e estatico por definicao). */
    private static final class Listener {
        @EventBus.Handler
        public void onRender(Events.Render2D event) {
            poll();
        }

        @EventBus.Handler
        public void onTick(Events.Tick event) {
            poll();
        }
    }
}
