package composite.problem;

import java.util.List;

// Holds Products only — a bundle inside a bundle isn't expressible.
public record Bundle(String name, int discountPercent, List<Product> products) {
}
