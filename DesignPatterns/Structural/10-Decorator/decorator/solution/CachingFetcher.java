package decorator.solution;

import java.util.HashMap;
import java.util.Map;

public class CachingFetcher implements Fetcher {
    private final Fetcher inner;
    private final Map<String, String> cache = new HashMap<>();

    public CachingFetcher(Fetcher inner) {
        this.inner = inner;
    }

    @Override
    public String fetch(String url) {
        String cached = cache.get(url);
        if (cached != null) {
            return cached;
        }
        String body = inner.fetch(url);    // failures aren't cached — they propagate
        cache.put(url, body);
        return body;
    }
}
