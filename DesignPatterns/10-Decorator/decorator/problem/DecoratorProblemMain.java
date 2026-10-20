package decorator.problem;

import java.util.Map;

public class DecoratorProblemMain {

    public static void main(String[] args) {
        FakeServer server = new FakeServer(Map.of("/orders", "[42]"));
        Fetcher fetcher = new CachingRetryingHttpFetcher(server);

        server.failNext(2);
        assert fetcher.fetch("/orders").equals("[42]");
        assert server.calls() == 3;            // 2 failures + 1 success
        assert fetcher.fetch("/orders").equals("[42]");
        assert server.calls() == 3;            // cache hit

        // Wanted: cache, but fail fast (no retry) for a health check. The only caching
        // class always retries, so one failure costs two calls.
        FakeServer health = new FakeServer(Map.of("/health", "ok"));
        health.failNext(1);
        assert new CachingRetryingHttpFetcher(health).fetch("/health").equals("ok");
        assert health.calls() == 2;
        System.out.println("can't get caching without retry — the combination is baked into the class tree");

        System.out.println("problem: all asserts passed");
    }
}
