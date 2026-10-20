package factory.solution;

import java.util.HashMap;
import java.util.Map;

// Pretend file storage: validates the path a real one would write under, keeps data in a map.
public class FileStorage implements Storage {
    private final String directory;
    private final Map<String, String> data = new HashMap<>();

    public FileStorage(String directory) {
        if (!directory.startsWith("/")) {
            throw new IllegalArgumentException("directory must be absolute: " + directory);
        }
        this.directory = directory;
    }

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
        return "file:" + directory;
    }
}
