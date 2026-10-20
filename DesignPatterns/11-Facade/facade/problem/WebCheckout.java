package facade.problem;

// Knows all four subsystems, the order to call them in, and how to undo each step.
public class WebCheckout {
    private final Inventory inventory;
    private final PaymentService payments;
    private final ShippingService shipping;
    private final Notifier notifier;

    public WebCheckout(Inventory inventory, PaymentService payments, ShippingService shipping, Notifier notifier) {
        this.inventory = inventory;
        this.payments = payments;
        this.shipping = shipping;
        this.notifier = notifier;
    }

    public String placeOrder(String email, String sku, int qty, int amount, String card, String address) {
        inventory.reserve(sku, qty);
        String txn;
        try {
            txn = payments.charge(card, amount);
        } catch (RuntimeException e) {
            inventory.release(sku, qty);
            throw e;
        }
        String shipment;
        try {
            shipment = shipping.ship(sku, qty, address);
        } catch (RuntimeException e) {
            payments.refund(txn);
            inventory.release(sku, qty);
            throw e;
        }
        notifier.send(email, "order shipped: " + shipment);
        return shipment;
    }
}
