package decorator.solution;

import java.util.List;

public class LoggingFetcher implements Fetcher {
    private final Fetcher inner;
    private final List<String> log;

    public LoggingFetcher(Fetcher inner, List<String> log) {
        this.inner = inner;
        this.log = log;
    }

    @Override
    public String fetch(String url) {
        log.add("GET " + url);
        return inner.fetch(url);
    }
}
