package factory.problem;

import java.util.HashMap;
import java.util.Map;

public class MemoryStorage implements Storage {
    private final Map<String, String> data = new HashMap<>();

    @Override
    public void put(String key, String value) {
        data.put(key, value);
    }

    @Override
    public String get(String key) {
        return data.get(key);
    }

    @Override
    public String describe() {
        return "memory";
    }
}
