package factory.solution;

public class CacheWarmer {
    private final Storage storage;

    public CacheWarmer(Storage storage) {
        this.storage = storage;
    }

    public String warm(String key) {
        return storage.get(key);
    }

    public String source() {
        return storage.describe();
    }
}
