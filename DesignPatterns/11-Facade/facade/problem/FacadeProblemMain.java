package facade.problem;

import java.util.Map;

public class FacadeProblemMain {

    public static void main(String[] args) {
        Inventory inventory = new Inventory(Map.of("BOOK", 5));
        PaymentService payments = new PaymentService();
        ShippingService shipping = new ShippingService();
        Notifier notifier = new Notifier();

        WebCheckout web = new WebCheckout(inventory, payments, shipping, notifier);
        MobileCheckout mobile = new MobileCheckout(inventory, payments, shipping, notifier);

        assert web.placeOrder("a@x.com", "BOOK", 1, 300, "4242", "Pune").equals("shp_1");
        assert inventory.available("BOOK") == 4;

        // Web handles a declined card correctly...
        expectFailure(() -> web.placeOrder("b@x.com", "BOOK", 1, 300, "declined", "Pune"));
        assert inventory.available("BOOK") == 4;

        // ...mobile leaks the reservation: 2 books gone with no order behind them.
        expectFailure(() -> mobile.placeOrder("c@x.com", "BOOK", 2, 600, "declined", "Pune"));
        assert inventory.available("BOOK") == 2;
        System.out.println("mobile leaked 2 reserved books on a declined card");

        // A new step (fraud check before charging) must be added to every copy.
        System.out.println("problem: all asserts passed");
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            assert false : "expected the order to fail";
        } catch (RuntimeException expected) {
            System.out.println("order failed: " + expected.getMessage());
        }
    }
}
