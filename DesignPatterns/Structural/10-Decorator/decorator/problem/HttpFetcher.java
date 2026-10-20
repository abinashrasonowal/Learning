package decorator.problem;

public class HttpFetcher implements Fetcher {
    private final FakeServer server;

    public HttpFetcher(FakeServer server) {
        this.server = server;
    }

    @Override
    public String fetch(String url) {
        return server.handle(url);
    }
}
