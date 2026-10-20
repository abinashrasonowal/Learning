package factory.solution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

// The one place that turns a URI into a Storage. Callers ask for "a Storage for this URI"
// and never name a concrete class. New scheme = one register() call, no edits here.
public class StorageFactory {
    private final Map<String, Function<String, Storage>> creators = new LinkedHashMap<>();

    public static StorageFactory withDefaults() {
        StorageFactory factory = new StorageFactory();
        factory.register("mem:", rest -> new MemoryStorage());
        factory.register("s3://", StorageFactory::s3);
        factory.register("file:", FileStorage::new);
        return factory;
    }

    public void register(String scheme, Function<String, Storage> creator) {
        creators.put(scheme, creator);
    }

    public Storage create(String uri) {
        for (Map.Entry<String, Function<String, Storage>> entry : creators.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue().apply(uri.substring(entry.getKey().length()));
            }
        }
        throw new IllegalArgumentException("unknown storage: " + uri + ", known: " + creators.keySet());
    }

    // Validation lives with creation — written once, so no caller can skip it.
    private static Storage s3(String bucket) {
        if (!bucket.matches("[a-z0-9.-]{3,63}")) {
            throw new IllegalArgumentException("invalid bucket name: " + bucket);
        }
        return new S3Storage(bucket);
    }
}
