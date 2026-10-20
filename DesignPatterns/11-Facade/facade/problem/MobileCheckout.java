package facade.problem;

// The same orchestration, copied for the mobile API. The copy forgot to release stock
// when the card is declined.
public class MobileCheckout {
    private final Inventory inventory;
    private final PaymentService payments;
    private final ShippingService shipping;
    private final Notifier notifier;

    public MobileCheckout(Inventory inventory, PaymentService payments, ShippingService shipping, Notifier notifier) {
        this.inventory = inventory;
        this.payments = payments;
        this.shipping = shipping;
        this.notifier = notifier;
    }

    public String placeOrder(String email, String sku, int qty, int amount, String card, String address) {
        inventory.reserve(sku, qty);
        String txn = payments.charge(card, amount);    // throws -> reserved stock is never released
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
