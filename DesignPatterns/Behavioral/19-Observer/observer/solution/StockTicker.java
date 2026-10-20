package observer.solution;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StockTicker {
    private final Map<String, Double> prices = new HashMap<>();
    // LinkedHashSet: keeps attach order, and attaching the same listener twice is a no-op.
    private final Set<PriceListener> listeners = new LinkedHashSet<>();

    public Subscription attach(PriceListener listener) {
        listeners.add(listener);
        return () -> detach(listener);
    }

    public void detach(PriceListener listener) {
        listeners.remove(listener);
    }

    public int listenerCount() {
        return listeners.size();
    }

    public void setPrice(String symbol, double price) {
        Double old = prices.put(symbol, price);
        if (old != null && old == price) {
            return;   // nothing changed — don't notify
        }
        notifyListeners(new PriceEvent(symbol, old == null ? Double.NaN : old, price));
    }

    private void notifyListeners(PriceEvent event) {
        // Iterate a snapshot: a listener may detach itself (or attach another) mid-notify.
        for (PriceListener listener : List.copyOf(listeners)) {
            try {
                listener.onPrice(event);
            } catch (RuntimeException e) {
                // One broken listener must not starve the rest.
                System.err.println("listener failed on " + event + ": " + e.getMessage());
            }
        }
    }
}
