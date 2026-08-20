package polar.ru.api.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import polar.ru.api.events.Event;
import polar.ru.api.events.EventLink;

public class EventInvoker {
    private static final ConcurrentHashMap<Class<?>, Object> classRegistry = new ConcurrentHashMap();
    private static final ConcurrentHashMap<Class<? extends Event>, List<Invocation>> invocationCache = new ConcurrentHashMap();
    private static final ConcurrentHashMap<String, Long> slowHandlerWarnings = new ConcurrentHashMap();
    private static final ConcurrentHashMap<String, Long> slowEventWarnings = new ConcurrentHashMap();
    private static final boolean PERF_DEBUG = Boolean.parseBoolean(System.getProperty("polar.perf.debug", "false"));
    private static final long SLOW_HANDLER_NANOS = Long.getLong("polar.perf.handlerMs", 8L) * 1000000L;
    private static final long SLOW_EVENT_NANOS = Long.getLong("polar.perf.eventMs", 18L) * 1000000L;
    private static final long WARN_COOLDOWN_NANOS = Long.getLong("polar.perf.cooldownMs", 1000L) * 1000000L;
    private static volatile boolean cacheDirty = true;

    public static void register(Object obj) {
        classRegistry.putIfAbsent(obj.getClass(), obj);
        cacheDirty = true;
    }

    public static void unregister(Object obj) {
        classRegistry.remove(obj.getClass());
        cacheDirty = true;
    }

    public static void clean() {
        classRegistry.clear();
        invocationCache.clear();
        cacheDirty = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void invoke(Event event) {
        long elapsed;
        List<Invocation> invocations;
        long eventStart;
        long l2 = eventStart = PERF_DEBUG ? System.nanoTime() : 0L;
        if (cacheDirty) {
            EventInvoker.rebuildCache();
        }
        if ((invocations = EventInvoker.resolveInvocations(event.getClass())) == null || invocations.isEmpty()) {
            return;
        }
        for (Invocation invocation : invocations) {
            if (!classRegistry.containsKey(invocation.listener().getClass())) continue;
            Method method = invocation.method();
            method.setAccessible(true);
            long handlerStart = PERF_DEBUG ? System.nanoTime() : 0L;
            try {
                method.invoke(invocation.listener(), event);
            }
            catch (Throwable ignored) {
            }
            finally {
                long elapsed2;
                if (!PERF_DEBUG || (elapsed2 = System.nanoTime() - handlerStart) < SLOW_HANDLER_NANOS) continue;
                EventInvoker.logSlowHandler(event, invocation, elapsed2);
            }
        }
        if (PERF_DEBUG && (elapsed = System.nanoTime() - eventStart) >= SLOW_EVENT_NANOS) {
            EventInvoker.logSlowEvent(event, elapsed, invocations.size());
        }
    }

    public static boolean hasListeners(Class<? extends Event> eventClass) {
        List<Invocation> invocations;
        if (cacheDirty) {
            EventInvoker.rebuildCache();
        }
        return (invocations = EventInvoker.resolveInvocations(eventClass)) != null && !invocations.isEmpty();
    }

    private static List<Invocation> resolveInvocations(Class<? extends Event> eventClass) {
        if (invocationCache.isEmpty()) {
            return List.of();
        }
        ArrayList<Invocation> resolved = new ArrayList<Invocation>();
        for (Map.Entry<Class<? extends Event>, List<Invocation>> entry : invocationCache.entrySet()) {
            if (!entry.getKey().isAssignableFrom(eventClass)) continue;
            resolved.addAll((Collection<Invocation>)entry.getValue());
        }
        if (resolved.isEmpty()) {
            return List.of();
        }
        resolved.sort((a2, b2) -> {
            int priorityCompare = Integer.compare(b2.priority(), a2.priority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            int classCompare = a2.listener().getClass().getName().compareTo(b2.listener().getClass().getName());
            if (classCompare != 0) {
                return classCompare;
            }
            return a2.method().getName().compareTo(b2.method().getName());
        });
        return resolved;
    }

    private static synchronized void rebuildCache() {
        if (!cacheDirty) {
            return;
        }
        ConcurrentHashMap<Class, List> rebuilt = new ConcurrentHashMap<Class, List>();
        for (Object listener : classRegistry.values()) {
            Class<?> clazz = listener.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Method method : clazz.getDeclaredMethods()) {
                    Class<?>[] parameters;
                    if (!method.isAnnotationPresent(EventLink.class) || (parameters = method.getParameterTypes()).length != 1 || !Event.class.isAssignableFrom(parameters[0])) continue;
                    Class<?> eventClass = parameters[0];
                    method.setAccessible(true);
                    rebuilt.computeIfAbsent(eventClass, key -> new ArrayList()).add(new Invocation(listener, method, method.getAnnotation(EventLink.class).priority()));
                }
                clazz = clazz.getSuperclass();
            }
        }
        for (List invocations : rebuilt.values()) {
            ((List<Invocation>) invocations).sort((a2, b2) -> {
                int priorityCompare = Integer.compare(b2.priority(), a2.priority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                int classCompare = a2.listener().getClass().getName().compareTo(b2.listener().getClass().getName());
                if (classCompare != 0) {
                    return classCompare;
                }
                return a2.method().getName().compareTo(b2.method().getName());
            });
        }
        invocationCache.clear();
        invocationCache.putAll((Map)rebuilt);
        cacheDirty = false;
    }

    private static void logSlowHandler(Event event, Invocation invocation, long elapsedNanos) {
        String listenerName = invocation.listener().getClass().getSimpleName();
        String methodName = invocation.method().getName();
        String eventName = event.getClass().getSimpleName();
        String key = "handler:" + eventName + ":" + listenerName + "#" + methodName;
        if (!EventInvoker.canWarn(slowHandlerWarnings, key)) {
            return;
        }
        System.out.println(String.format(Locale.ROOT, "[PerfDebug] Slow handler: %s -> %s#%s took %.2f ms", eventName, listenerName, methodName, (double)elapsedNanos / 1000000.0));
    }

    private static void logSlowEvent(Event event, long elapsedNanos, int invocationCount) {
        String eventName = event.getClass().getSimpleName();
        String key = "event:" + eventName;
        if (!EventInvoker.canWarn(slowEventWarnings, key)) {
            return;
        }
        System.out.println(String.format(Locale.ROOT, "[PerfDebug] Slow event: %s took %.2f ms for %d handlers", eventName, (double)elapsedNanos / 1000000.0, invocationCount));
    }

    private static boolean canWarn(ConcurrentHashMap<String, Long> warnings, String key) {
        long now = System.nanoTime();
        Long lastWarn = warnings.get(key);
        if (lastWarn != null && now - lastWarn < WARN_COOLDOWN_NANOS) {
            return false;
        }
        warnings.put(key, now);
        return true;
    }

    private record Invocation(Object listener, Method method, int priority) {
    }
}

