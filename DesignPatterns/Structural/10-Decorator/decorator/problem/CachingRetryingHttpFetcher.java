package decorator.problem;

import java.util.HashMap;
import java.util.Map;

// One class per combination. Caching WITHOUT retry? Another class. Add logging? Another
// level — and then every combination of logging/caching/retry is its own subclass.
public class CachingRetryingHttpFetcher extends RetryingHttpFetcher {
    private final Map<String, String> cache = new HashMap<>();

    public CachingRetryingHttpFetcher(FakeServer server) {
        super(server);
    }

    @Override
    public String fetch(String url) {
        String cached = cache.get(url);
        if (cached != null) {
            return cached;
        }
        String body = super.fetch(url);
        cache.put(url, body);
        return body;
    }
}
