package builder.solution;

public class BuilderSolutionMain {

    public static void main(String[] args) {
        HttpRequest simple = HttpRequest.builder("GET", "https://api.example.com/orders").build();
        assert simple.timeoutMs() == 5000 && simple.retries() == 0;

        // Every argument is named at the call site — the swap from problem/ can't hide.
        HttpRequest.Builder builder = HttpRequest.builder("POST", "https://api.example.com/orders")
                .header("Content-Type", "application/json")
                .body("{}")
                .retries(3)
                .timeoutMs(2000);
        HttpRequest post = builder.build();
        assert post.timeoutMs() == 2000 && post.retries() == 3;

        // Changing the builder after build() doesn't reach the built request.
        builder.header("Authorization", "Bearer leaked");
        assert !post.headers().containsKey("Authorization");
        assert builder.build().headers().containsKey("Authorization");

        // And the built request's headers can't be changed at all.
        try {
            post.headers().put("X-Debug", "1");
            assert false : "expected UnsupportedOperationException";
        } catch (UnsupportedOperationException expected) {
            System.out.println("headers are read-only");
        }

        // Headers but default timeout — no new constructor needed, just leave it out.
        HttpRequest withHeader = HttpRequest.builder("GET", "https://api.example.com/orders")
                .header("Accept", "application/json")
                .build();
        assert withHeader.timeoutMs() == 5000 && withHeader.headers().size() == 1;

        // build() validates the whole object, including rules across fields.
        expectRejected(() -> HttpRequest.builder("GET", "https://api.example.com").body("{}").build());
        expectRejected(() -> HttpRequest.builder("FETCH", "https://api.example.com").build());
        expectRejected(() -> HttpRequest.builder("POST", "ftp://files").build());
        expectRejected(() -> HttpRequest.builder("GET", "https://api.example.com").timeoutMs(0).build());
        expectRejected(() -> HttpRequest.builder("GET", "https://api.example.com").retries(2000).build());

        // toBuilder(): a variant of an existing request, original untouched.
        HttpRequest slow = post.toBuilder().timeoutMs(30_000).build();
        assert slow.timeoutMs() == 30_000 && slow.body().equals("{}");
        assert post.timeoutMs() == 2000;

        System.out.println("solution: all asserts passed");
    }

    private static void expectRejected(Runnable action) {
        try {
            action.run();
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }
    }
}
