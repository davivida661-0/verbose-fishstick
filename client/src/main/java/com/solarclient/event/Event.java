package com.solarclient.event;

/**
 * Base de todos os eventos do client.
 *
 * <p>Um evento e "postado" por um mixin (por exemplo, o mixin do
 * {@code GuiIngame} posta {@link Events.Render2D}) e entregue a todos os metodos
 * anotados com {@link EventBus.Handler}. Como o evento e cancelavel, o listener
 * pode impedir o comportamento original do jogo com
 * {@link #setCancelled(boolean)}.</p>
 */
public class Event {

    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
