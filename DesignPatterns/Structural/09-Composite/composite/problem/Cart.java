package composite.problem;

import java.util.ArrayList;
import java.util.List;

// Treats each kind differently, so every operation is an instanceof chain — and a kind it
// doesn't list falls through silently.
public class Cart {
    private final List<Object> items = new ArrayList<>();

    public void add(Object item) {
        items.add(item);
    }

    public int total() {
        int total = 0;
        for (Object item : items) {
            if (item instanceof Product p) {
                total += p.price();
            } else if (item instanceof Bundle b) {
                int sum = 0;
                for (Product p : b.products()) {
                    sum += p.price();
                }
                total += sum * (100 - b.discountPercent()) / 100;
            }
            // GiftCard? not listed -> contributes nothing, no error
        }
        return total;
    }
}
