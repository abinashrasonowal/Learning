package decorator.solution;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

// Stands in for the network: counts calls and can fail the next N on demand.
public class FakeServer {
    private final Map<String, String> pages;
    private int calls;
    private int failuresLeft;

    public FakeServer(Map<String, String> pages) {
        this.pages = pages;
    }

    public void failNext(int n) {
        failuresLeft = n;
    }

    public String handle(String url) {
        calls++;
        if (failuresLeft > 0) {
            failuresLeft--;
            throw new UncheckedIOException(new IOException("503 for " + url));
        }
        String body = pages.get(url);
        if (body == null) {
            throw new IllegalArgumentException("404 for " + url);
        }
        return body;
    }

    public int calls() {
        return calls;
    }
}
