package decorator.problem;

import java.io.UncheckedIOException;

// Behaviour added by subclassing: retry is welded onto HTTP, and the 3 is hard-coded.
public class RetryingHttpFetcher extends HttpFetcher {

    public RetryingHttpFetcher(FakeServer server) {
        super(server);
    }

    @Override
    public String fetch(String url) {
        UncheckedIOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return super.fetch(url);
            } catch (UncheckedIOException e) {
                last = e;
            }
        }
        throw last;
    }
}
