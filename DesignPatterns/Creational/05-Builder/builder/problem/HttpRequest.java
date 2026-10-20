package builder.problem;

import java.util.Map;

// Telescoping constructors: one per combination someone needed. Two ints side by side
// means a swapped call compiles. The headers map is stored as given — the caller still
// owns it. Nothing checks that a GET has no body.
public class HttpRequest {
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;
    private final int timeoutMs;
    private final int retries;

    public HttpRequest(String method, String url) {
        this(method, url, Map.of(), null, 5000, 0);
    }

    public HttpRequest(String method, String url, String body) {
        this(method, url, Map.of(), body, 5000, 0);
    }

    public HttpRequest(String method, String url, Map<String, String> headers, String body,
                       int timeoutMs, int retries) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
        this.timeoutMs = timeoutMs;
        this.retries = retries;
    }

    public String method() {
        return method;
    }

    public String url() {
        return url;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String body() {
        return body;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public int retries() {
        return retries;
    }
}
