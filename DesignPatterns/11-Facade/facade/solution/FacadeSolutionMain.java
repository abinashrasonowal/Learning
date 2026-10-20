package facade.solution;

import java.util.List;
import java.util.Map;

public class FacadeSolutionMain {

    public static void main(String[] args) {
        Inventory inventory = new Inventory(Map.of("BOOK", 5));
        PaymentService payments = new PaymentService();
        ShippingService shipping = new ShippingService();
        Notifier notifier = new Notifier();
        OrderFacade orders = new OrderFacade(inventory, payments, shipping, notifier);

        // Web and mobile both call the facade — one orchestration, no copies.
        assert orders.placeOrder(new OrderRequest("a@x.com", "BOOK", 1, 300, "4242", "Pune")).equals("shp_1");
        assert inventory.available("BOOK") == 4;
        assert notifier.sent().equals(List.of("a@x.com: order shipped: shp_1"));

        // The case mobile leaked in problem/ — every caller now releases the stock.
        expectFailure(() -> orders.placeOrder(new OrderRequest("c@x.com", "BOOK", 2, 600, "declined", "Pune")));
        assert inventory.available("BOOK") == 4;

        // Shipping fails after charging: refund and release, no email.
        expectFailure(() -> orders.placeOrder(new OrderRequest("d@x.com", "BOOK", 1, 300, "4242", " ")));
        assert payments.refunded().equals(List.of("txn_2"));
        assert inventory.available("BOOK") == 4;
        assert notifier.sent().size() == 1;

        // Out of stock fails before anything is charged.
        expectFailure(() -> orders.placeOrder(new OrderRequest("e@x.com", "BOOK", 99, 9900, "4242", "Pune")));
        assert payments.refunded().size() == 1;

        // The facade simplifies; it doesn't lock the subsystems away.
        inventory.release("BOOK", 1);
        assert inventory.available("BOOK") == 5;

        System.out.println("solution: all asserts passed");
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
