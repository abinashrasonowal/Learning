package observer.solution;

import java.util.ArrayList;
import java.util.List;

// The textbook version: plain ArrayList, plain for-each. Kept only to show what breaks.
public class NaiveStockTicker {
    private final List<PriceListener> listeners = new ArrayList<>();

    public void attach(PriceListener listener) {
        listeners.add(listener);
    }

    public void detach(PriceListener listener) {
        listeners.remove(listener);
    }

    public void setPrice(String symbol, double price) {
        PriceEvent event = new PriceEvent(symbol, Double.NaN, price);
        for (PriceListener listener : listeners) {   // a detach inside onPrice() -> ConcurrentModificationException
            listener.onPrice(event);
        }
    }
}
j