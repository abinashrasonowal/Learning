package builder.problem;

import java.util.HashMap;
import java.util.Map;

public class BuilderProblemMain {

    public static void main(String[] args) {
        HttpRequest simple = new HttpRequest("GET", "https://api.example.com/orders");
        assert simple.timeoutMs() == 5000;

        // Meant "retry 3 times, 2 s timeout". Read the call site — can you tell it's wrong?
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        HttpRequest post = new HttpRequest("POST", "https://api.example.com/orders", headers, "{}", 3, 2000);
        assert post.timeoutMs() == 3 && post.retries() == 2000;
        System.out.println("swapped ints compiled: timeout=3 ms, retries=2000");

        // The "immutable" request changes when the caller reuses its map.
        headers.put("Authorization", "Bearer leaked");
        assert post.headers().containsKey("Authorization");
        System.out.println("header added after construction leaked into the request");

        // No validation anywhere: a GET with a body is accepted.
        HttpRequest odd = new HttpRequest("GET", "https://api.example.com/orders", "{}");
        assert odd.body() != null;
        System.out.println("GET with a body accepted");

        // Want headers but default timeout? There's no constructor for that — add a 4th.
        System.out.println("problem: all asserts passed");
    }
}
