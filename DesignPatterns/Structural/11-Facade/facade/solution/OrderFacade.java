package facade.solution;

// One simple entry point over four subsystems. The order of calls and the undo steps
// are written here once; web, mobile and batch callers all get them for free.
// Subsystems are injected, not new-ed here, so tests can hand in fakes.
public class OrderFacade {
    private final Inventory inventory;
    private final PaymentService payments;
    private final ShippingService shipping;
    private final Notifier notifier;

    public OrderFacade(Inventory inventory, PaymentService payments, ShippingService shipping, Notifier notifier) {
        this.inventory = inventory;
        this.payments = payments;
        this.shipping = shipping;
        this.notifier = notifier;
    }

    public String placeOrder(OrderRequest order) {
        inventory.reserve(order.sku(), order.qty());
        String txn;
        try {
            txn = payments.charge(order.card(), order.amount());
        } catch (RuntimeException e) {
            inventory.release(order.sku(), order.qty());
            throw e;
        }
        String shipment;
        try {
            shipment = shipping.ship(order.sku(), order.qty(), order.address());
        } catch (RuntimeException e) {
            payments.refund(txn);
            inventory.release(order.sku(), order.qty());
            throw e;
        }
        notifier.send(order.email(), "order shipped: " + shipment);
        return shipment;
    }
}
