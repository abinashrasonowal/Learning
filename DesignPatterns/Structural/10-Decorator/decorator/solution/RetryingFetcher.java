package decorator.solution;

import java.io.UncheckedIOException;

// A decorator: IS a Fetcher, HAS a Fetcher. Wraps any fetcher — HTTP, cached, logged.
public class RetryingFetcher implements Fetcher {
    private final Fetcher inner;
    private final int attempts;

    public RetryingFetcher(Fetcher inner, int attempts) {
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be >= 1, got " + attempts);
        }
        this.inner = inner;
        this.attempts = attempts;
    }

    @Override
    public String fetch(String url) {
        UncheckedIOException last = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                return inner.fetch(url);
            } catch (UncheckedIOException e) {
                last = e;
            }
        }
        throw last;
    }
}
