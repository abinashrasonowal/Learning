package factory.solution;

import java.util.HashMap;
import java.util.Map;

// Pretend S3: keeps data in a map, but carries the bucket name a real client would need.
public class S3Storage implements Storage {
    private final String bucket;
    private final Map<String, String> data = new HashMap<>();

    public S3Storage(String bucket) {
        this.bucket = bucket;
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
        return "s3:" + bucket;
    }
}
