package builder.solution;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// Immutable. The only way in is the Builder, and build() is the only place that validates.
public final class HttpRequest {
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "DELETE");

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;
    private final int timeoutMs;
    private final int retries;

    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.url = builder.url;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));   // defensive copy
        this.body = builder.body;
        this.timeoutMs = builder.timeoutMs;
        this.retries = builder.retries;
    }

    // Required parameters go here, so a request without method + url can't even be started.
    public static Builder builder(String method, String url) {
        return new Builder(method, url);
    }

    // Start a variant from an existing request without touching the original.
    public Builder toBuilder() {
        Builder builder = new Builder(method, url).timeoutMs(timeoutMs).retries(retries);
        builder.headers.putAll(headers);
        builder.body = body;
        return builder;
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

    public static final class Builder {
        private final String method;
        private final String url;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String body;
        private int timeoutMs = 5000;   // defaults live in one place
        private int retries;

        private Builder(String method, String url) {
            this.method = method;
            this.url = url;
        }

        // Accumulates — call it once per header instead of building a map up front.
        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder timeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder retries(int retries) {
            this.retries = retries;
            return this;
        }

        // Cross-field rules can only be checked once every field is known — i.e. here.
        public HttpRequest build() {
            if (!METHODS.contains(method)) {
                throw new IllegalArgumentException("unsupported method: " + method);
            }
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                throw new IllegalArgumentException("url must be http(s): " + url);
            }
            if (body != null && (method.equals("GET") || method.equals("DELETE"))) {
                throw new IllegalArgumentException(method + " must not have a body");
            }
            if (timeoutMs <= 0) {
                throw new IllegalArgumentException("timeoutMs must be positive, got " + timeoutMs);
            }
            if (retries < 0 || retries > 5) {
                throw new IllegalArgumentException("retries must be 0..5, got " + retries);
            }
            return new HttpRequest(this);
        }
    }
}
