package zov.alphadlc.util.friend;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

final class CaseInsensitiveNameIndex<T> {
    private final Function<T, String> nameExtractor;
    private final Map<String, T> valuesByName = new LinkedHashMap<>();

    CaseInsensitiveNameIndex(Function<T, String> nameExtractor) {
        this.nameExtractor = nameExtractor;
    }

    synchronized boolean add(T value) {
        String key = keyOf(value);
        if (key == null || valuesByName.containsKey(key)) return false;
        valuesByName.put(key, value);
        return true;
    }

    synchronized T remove(String name) {
        return name == null ? null : valuesByName.remove(normalize(name));
    }

    synchronized boolean contains(String name) {
        return name != null && valuesByName.containsKey(normalize(name));
    }

    synchronized T get(String name) {
        return name == null ? null : valuesByName.get(normalize(name));
    }

    synchronized void replaceAll(Collection<? extends T> values) {
        valuesByName.clear();
        for (T value : values) add(value);
    }

    synchronized void clear() {
        valuesByName.clear();
    }

    synchronized List<T> values() {
        return List.copyOf(valuesByName.values());
    }

    private String keyOf(T value) {
        if (value == null) return null;
        String name = nameExtractor.apply(value);
        return name == null ? null : normalize(name);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
