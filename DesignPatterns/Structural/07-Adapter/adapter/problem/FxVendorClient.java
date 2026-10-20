package adapter.problem;

import java.util.Map;

// Third-party SDK — we can't edit it. Quotes only USD-based pairs ("USDINR"), returns the
// rate scaled by 10_000 as a long, and signals "unknown pair" with -1 instead of throwing.
public class FxVendorClient {
    private static final Map<String, Long> QUOTES = Map.of(
            "USDINR", 832_500L,     // 83.25
            "USDEUR", 9_200L);      // 0.92
    private int calls;

    public long getQuote(String pair) {
        calls++;
        return QUOTES.getOrDefault(pair, -1L);
    }

    public int calls() {
        return calls;
    }
}
