package com.solarclient.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>Acesso reflexivo com cache</h1>
 *
 * <p>Alguns objetos internos do 1.8.9 sao <i>privados</i> e o nome do campo
 * muda dependendo do mapping usado (MCP, SRG, yarn). Em vez de chutar e
 * quebrar, este utilizador procura o primeiro campo cujo tipo casa e guarda o
 * {@link Field} em cache.</p>
 *
 * <p>Exemplo: pegar o {@code RenderManager} (que fica dentro do
 * {@code Minecraft} ou do {@code EntityRenderer} dependendo da versao) para
 * desenhar NameTags.</p>
 *
 * <p>Se o objeto nao for encontrado, devolve {@code null} e o mod simplesmente
 * nao desenha - nunca lanca excecao em tempo de execucao.</p>
 */
public final class Reflect {

    private static final Map<String, Field> CACHE = new HashMap<>();
    private static final Map<String, Method> METHOD_CACHE = new HashMap<>();

    private Reflect() {
    }

    /**
     * Procura (e devolve) o valor de um campo cujo tipo seja {@code type}.
     *
     * @return null se nao existir
     */
    public static <T> T get(Object owner, Class<T> type) {
        if (owner == null || type == null) {
            return null;
        }
        Field field = findField(owner.getClass(), type);
        if (field == null) {
            return null;
        }
        try {
            return type.cast(field.get(owner));
        } catch (Exception e) {
            return null;
        }
    }

    /** Igual {@link #get} mas em um campo estatico. */
    public static <T> T staticGet(Class<?> ownerClass, Class<T> type) {
        Field field = findField(ownerClass, type);
        if (field == null || !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            return null;
        }
        try {
            return type.cast(field.get(null));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Chama o primeiro metodo (sem argumentos) cujo nomeeste na lista de
     * candidatos e cujo tipo de retorno seja {@code returnType}.
     *
     * <p>Usado em objetos internos cujo nome de metodo muda entre versoes, como
     * a barra de boss do 1.8.9.</p>
     *
     * @return null se nenhum candidato existir
     */
    public static Object call(Object owner, Class<?> returnType, String... names) {
        if (owner == null) {
            return null;
        }
        for (String name : names) {
            try {
                Method method = owner.getClass().getMethod(name);
                if (returnType == null || returnType.isAssignableFrom(method.getReturnType())) {
                    return method.invoke(owner);
                }
            } catch (Exception ignored) {
                // tenta o proximo candidato
            }
        }
        return null;
    }

    /** Procura um metodo publico sem argumento que devolve {@code type}. */
    public static <T> T callGetter(Object owner, Class<T> type) {
        if (owner == null) {
            return null;
        }
        String key = owner.getClass().getName() + "#" + type.getName();
        Method method = METHOD_CACHE.get(key);
        if (method == null) {
            for (Method m : owner.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && type.isAssignableFrom(m.getReturnType())) {
                    method = m;
                    METHOD_CACHE.put(key, m);
                    break;
                }
            }
            if (method == null) {
                METHOD_CACHE.put(key, null);
                return null;
            }
        }
        try {
            return type.cast(method.invoke(owner));
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ interno
    private static Field findField(Class<?> ownerClass, Class<?> type) {
        String key = ownerClass.getName() + "#" + type.getName();
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        Field found = null;
        for (Field field : ownerClass.getDeclaredFields()) {
            if (type.isAssignableFrom(field.getType())) {
                found = field;
                break;
            }
        }
        if (found != null) {
            found.setAccessible(true);
        }
        CACHE.put(key, found);
        return found;
    }
}
