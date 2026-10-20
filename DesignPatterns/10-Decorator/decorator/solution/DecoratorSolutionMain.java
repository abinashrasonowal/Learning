package decorator.solution;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DecoratorSolutionMain {

    public static void main(String[] args) {
        Map<String, String> pages = Map.of("/orders", "[42]", "/health", "ok");

        // Same behaviour as problem/'s CachingRetryingHttpFetcher — composed, not subclassed.
        FakeServer server = new FakeServer(pages);
        Fetcher cachedRetrying = new CachingFetcher(new RetryingFetcher(new HttpFetcher(server), 3));
        server.failNext(2);
        assert cachedRetrying.fetch("/orders").equals("[42]");
        assert server.calls() == 3;
        assert cachedRetrying.fetch("/orders").equals("[42]");
        assert server.calls() == 3;

        // The combination problem/ couldn't express: cache, but fail fast.
        FakeServer health = new FakeServer(pages);
        Fetcher cachedOnly = new CachingFetcher(new HttpFetcher(health));
        health.failNext(1);
        expectFailure(() -> cachedOnly.fetch("/health"));
        assert health.calls() == 1;

        // Retry gives up after its own limit — no hard-coded 3.
        FakeServer flaky = new FakeServer(pages);
        flaky.failNext(5);
        expectFailure(() -> new RetryingFetcher(new HttpFetcher(flaky), 2).fetch("/orders"));
        assert flaky.calls() == 2;

        // Order matters. Logging outside the cache sees every request...
        FakeServer s1 = new FakeServer(pages);
        List<String> outerLog = new ArrayList<>();
        Fetcher logThenCache = new LoggingFetcher(new CachingFetcher(new HttpFetcher(s1)), outerLog);
        logThenCache.fetch("/orders");
        logThenCache.fetch("/orders");
        assert outerLog.size() == 2 && s1.calls() == 1;

        // ...logging inside the cache sees only misses — i.e. real network calls.
        FakeServer s2 = new FakeServer(pages);
        List<String> innerLog = new ArrayList<>();
        Fetcher cacheThenLog = new CachingFetcher(new LoggingFetcher(new HttpFetcher(s2), innerLog));
        cacheThenLog.fetch("/orders");
        cacheThenLog.fetch("/orders");
        assert innerLog.size() == 1 && s2.calls() == 1;

        // One-method interface: a throwaway decorator can be a lambda.
        Fetcher base = new HttpFetcher(new FakeServer(pages));
        Fetcher shouting = url -> base.fetch(url).toUpperCase();
        assert shouting.fetch("/health").equals("OK");

        System.out.println("solution: all asserts passed");
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            assert false : "expected UncheckedIOException";
        } catch (UncheckedIOException expected) {
            System.out.println("failed as expected: " + expected.getCause().getMessage());
        }
    }
}
