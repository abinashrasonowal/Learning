package flyweight.solution;

import java.util.HashMap;
import java.util.Map;

// Hands out one shared TreeType per (name, color). The only place TreeTypes are created.
public class TreeTypeFactory {
    private record Key(String name, String color) {
    }

    private final TextureLoader loader;
    private final Map<Key, TreeType> types = new HashMap<>();

    public TreeTypeFactory(TextureLoader loader) {
        this.loader = loader;
    }

    public TreeType get(String name, String color) {
        return types.computeIfAbsent(new Key(name, color),
                key -> new TreeType(key.name(), key.color(), loader.load(key.name())));
    }

    public int distinctTypes() {
        return types.size();
    }

    public long textureBytes() {
        long total = 0;
        for (TreeType type : types.values()) {
            total += type.textureBytes();
        }
        return total;
    }
}
