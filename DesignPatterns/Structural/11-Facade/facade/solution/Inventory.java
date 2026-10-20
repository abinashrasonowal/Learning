package facade.solution;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private final Map<String, Integer> stock = new HashMap<>();

    public Inventory(Map<String, Integer> initial) {
        stock.putAll(initial);
    }

    public void reserve(String sku, int qty) {
        int have = stock.getOrDefault(sku, 0);
        if (have < qty) {
            throw new IllegalStateException("out of stock: " + sku);
        }
        stock.put(sku, have - qty);
    }

    public void release(String sku, int qty) {
        stock.merge(sku, qty, Integer::sum);
    }

    public int available(String sku) {
        return stock.getOrDefault(sku, 0);
    }
}
