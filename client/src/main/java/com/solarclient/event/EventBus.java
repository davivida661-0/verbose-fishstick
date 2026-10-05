package com.solarclient.event;

import com.solarclient.util.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Barramento de eventos minimalista (zero dependencias).
 *
 * <p>Um listener e qualquer objeto com metodos anotados com {@link Handler} e um
 * unico parametro que herda de {@link Event}:</p>
 *
 * <pre>{@code
 * public class MeuMod {
 *     @EventBus.Handler
 *     public void onTick(Events.Tick e) { ... }
 * }
 * bus.register(new MeuMod());
 * }</pre>
 *
 * <p>A reflection acontece uma unica vez (no register). No {@link #post} fica so
 * um cast + {@code invoke}, o que mantem o custo por tick irrelevante mesmo
 * com 40 mods ligados.</p>
 */
public final class EventBus {

    /** Anota um metodo como handler de evento. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Handler {
    }

    private final Map<Class<? extends Event>, List<Invocation>> handlers = new HashMap<>();
    private final List<Object> listeners = new ArrayList<>();

    /** Registra todos os handlers de um objeto. */
    public void register(Object listener) {
        if (listener == null || listeners.contains(listener)) {
            return;
        }
        listeners.add(listener);

        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(Handler.class) == null) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
                Logger.warn("Handler invalido em " + listener.getClass().getSimpleName()
                        + "#" + method.getName() + " (esperado 1 parametro do tipo Event)");
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Event> type = (Class<? extends Event>) params[0];
            method.setAccessible(true);

            List<Invocation> list = handlers.get(type);
            if (list == null) {
                list = new ArrayList<>();
                handlers.put(type, list);
            }
            list.add(new Invocation(listener, method));
        }
    }

    public void unregister(Object listener) {
        listeners.remove(listener);
        for (Map.Entry<Class<? extends Event>, List<Invocation>> entry : handlers.entrySet()) {
            List<Invocation> kept = new ArrayList<>();
            for (Invocation inv : entry.getValue()) {
                if (inv.target != listener) {
                    kept.add(inv);
                }
            }
            entry.setValue(kept);
        }
    }

    /**
     * Entrega o evento a todos os handlers registrados.
     *
     * @return o proprio evento (ja modificado/cancelado), para uso encadeado
     */
    public <T extends Event> T post(T event) {
        List<Invocation> list = handlers.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return event;
        }
        for (int i = 0; i < list.size(); i++) {
            Invocation inv = list.get(i);
            try {
                inv.method.invoke(inv.target, event);
            } catch (Throwable t) {
                // Um mod com bug nunca pode derrubar o jogo.
                Logger.error("Erro no handler " + inv.method.getName() + " de "
                        + inv.target.getClass().getSimpleName(), t);
            }
        }
        return event;
    }

    public void clear() {
        handlers.clear();
        listeners.clear();
    }

    public List<Object> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    private static final class Invocation {
        private final Object target;
        private final Method method;

        private Invocation(Object target, Method method) {
            this.target = target;
            this.method = method;
        }
    }
}
