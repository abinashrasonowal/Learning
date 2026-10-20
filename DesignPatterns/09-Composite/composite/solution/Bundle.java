package composite.solution;

import java.util.ArrayList;
import java.util.List;

// Composite: holds Items — products, gift cards, or other bundles — and answers each
// operation by asking its children, then applying its own rule (the discount).
public class Bundle implements Item {
    private final String name;
    private final int discountPercent;
    private final List<Item> children = new ArrayList<>();

    public Bundle(String name, int discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("discount must be 0..100, got " + discountPercent);
        }
        this.name = name;
        this.discountPercent = discountPercent;
    }

    // add() lives here, not on Item — the "safe" composite: you can't add to a Product.
    public Bundle add(Item item) {
        children.add(item);
        return this;
    }

    @Override
    public int price() {
        int sum = 0;
        for (Item child : children) {
            sum += child.price();
        }
        return sum * (100 - discountPercent) / 100;
    }

    @Override
    public int productCount() {
        int count = 0;
        for (Item child : children) {
            count += child.productCount();
        }
        return count;
    }

    @Override
    public void describe(StringBuilder out, String indent) {
        out.append(indent).append(name);
        if (discountPercent > 0) {
            out.append(" (-").append(discountPercent).append("%)");
        }
        out.append(' ').append(price()).append('\n');
        for (Item child : children) {
            child.describe(out, indent + "  ");
        }
    }
}
