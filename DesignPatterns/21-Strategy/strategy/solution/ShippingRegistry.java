package strategy.solution;

import java.util.HashMap;
import java.util.Map;

// The if/else from problem/ didn't vanish — it moved here, to the edge, as a map lookup.
public class ShippingRegistry {
    private final Map<String, ShippingStrategy> strategies = new HashMap<>();

    public void register(String carrier, ShippingStrategy strategy) {
        strategies.put(carrier, strategy);
    }

    public ShippingCalculator calculatorFor(String carrier) {
        ShippingStrategy strategy = strategies.get(carrier);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown carrier: " + carrier + ", known: " + strategies.keySet());
        }
        return new ShippingCalculator(strategy);
    }
}
