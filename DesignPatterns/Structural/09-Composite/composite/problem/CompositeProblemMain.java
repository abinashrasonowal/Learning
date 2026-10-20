package composite.problem;

import java.util.List;

public class CompositeProblemMain {

    public static void main(String[] args) {
        Cart cart = new Cart();
        cart.add(new Product("Mouse", 500));
        cart.add(new Bundle("Desk setup", 10, List.of(new Product("Keyboard", 2000), new Product("Monitor", 8000))));
        assert cart.total() == 500 + 9000;

        // The new kind compiles, goes in the cart, and is ignored by total().
        cart.add(new GiftCard(1000));
        assert cart.total() == 9500;
        System.out.println("gift card silently ignored by total()");

        // "Desk setup + a cable bundle inside it" — no way to write it: Bundle holds Products only.
        // Adding count() or describe() means another instanceof chain like total().
        System.out.println("problem: all asserts passed");
    }
}
