package forge.util;

import arc.func.Prov;
import arc.struct.ObjectMap;

public class ArrayUtils {

    public static <K, V> V getOrDefault(ObjectMap<K, V> map, K key, Prov<V> def) {
        V value = map.get(key);
        if (value == null) {
            value = def.get();
            map.put(key, value);
        }
        return value;
    }
}
