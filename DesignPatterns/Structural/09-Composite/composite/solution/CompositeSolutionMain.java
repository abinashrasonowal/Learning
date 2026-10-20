package composite.solution;

public class CompositeSolutionMain {

    public static void main(String[] args) {
        // The cart is just a bundle with no discount — the root of the tree.
        Bundle cart = new Bundle("Cart", 0)
                .add(new Product("Mouse", 500))
                .add(new Bundle("Desk setup", 10)
                        .add(new Product("Keyboard", 2000))
                        .add(new Product("Monitor", 8000)));

        // Same total as problem/.
        assert cart.price() == 9500;

        // The gift card counts now — it implements Item, so the tree asks it like anything else.
        cart.add(new GiftCard(1000));
        assert cart.price() == 10_500;

        // Nesting: a bundle inside a bundle, discounts applied at each level.
        Bundle cables = new Bundle("Cables", 50)
                .add(new Product("HDMI", 400))
                .add(new Product("USB-C", 600));   // 1000 -> 500
        Bundle workstation = new Bundle("Workstation", 20)
                .add(new Product("Laptop", 50_000))
                .add(cables);                       // 50_500 -> 40_400
        cart.add(workstation);
        assert cart.price() == 10_500 + 40_400;

        // A new operation (productCount) recurses the same way — gift cards count 0.
        assert cart.productCount() == 3 + 3;

        StringBuilder out = new StringBuilder();
        cart.describe(out, "");
        assert out.toString().equals("""
                Cart 50900
                  Mouse 500
                  Desk setup (-10%) 9000
                    Keyboard 2000
                    Monitor 8000
                  Gift card 1000
                  Workstation (-20%) 40400
                    Laptop 50000
                    Cables (-50%) 500
                      HDMI 400
                      USB-C 600
                """);
        System.out.print(out);

        // A single product and a whole tree are used the same way.
        Item one = new Product("Pen", 20);
        Item many = cart;
        assert one.price() + many.price() == 50_920;

        System.out.println("solution: all asserts passed");
    }
}
