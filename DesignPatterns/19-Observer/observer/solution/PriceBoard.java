package observer.solution;

import java.util.HashMap;
import java.util.Map;

public class PriceBoard implements PriceListener {
    private final Map<String, Double> latest = new HashMap<>();

    @Override
    public void onPrice(PriceEvent event) {
        latest.put(event.symbol(), event.newPrice());
        System.out.println("[board] " + event.symbol() + " = " + event.newPrice());
    }

    public double price(String symbol) {
        return latest.get(symbol);
    }
}
