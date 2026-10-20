package prototype.solution;

import java.util.HashMap;
import java.util.Map;

// Named, pre-configured prototypes. Stores a copy on the way in and hands out a copy on
// the way out, so nobody can edit the stored template.
public class PrototypeRegistry<T extends Prototype<T>> {
    private final Map<String, T> prototypes = new HashMap<>();

    public void register(String key, T prototype) {
        prototypes.put(key, prototype.copy());
    }

    public T create(String key) {
        T prototype = prototypes.get(key);
        if (prototype == null) {
            throw new IllegalArgumentException("no prototype " + key + ", known: " + prototypes.keySet());
        }
        return prototype.copy();
    }
}
