// EXERCISE — one god class violating all five principles.
// Run: java -ea Exercise.java   (works, but it's a mess)
//
// Your job: rewrite this as Solution.java, from scratch, without peeking at
// SRP/OCP/LSP/ISP/DIP.java. Same output, no smells.
//
// Find all five before you start. Hints are at the bottom — don't scroll yet.

public class Exercise {
    public static void main(String[] args) {
        OrderService service = new OrderService();
        service.placeOrder("ORD-1", 1200.0, "STANDARD");
        service.placeOrder("ORD-2", 4999.0, "EXPRESS");

        System.out.println("\n-- archiving --");
        Storage storage = new FileStorage();
        storage.save("ORD-1");
        try {
            storage.archive("ORD-1");
        } catch (UnsupportedOperationException e) {
            System.out.println("Blew up: " + e.getMessage());
        }

        System.out.println("\n-- shipping --");
        StandardShipping shipping = new InStorePickup();
        shipping.dispatch("ORD-2");
        try {
            shipping.trackParcel("ORD-2");
        } catch (UnsupportedOperationException e) {
            System.out.println("Blew up: " + e.getMessage());
        }
    }
}

class OrderService {
    private final MySqlDatabase database;

    OrderService() {
        this.database = new MySqlDatabase();
    }

    void placeOrder(String orderId, double amount, String shippingType) {
        double shippingCost;
        if (shippingType.equals("STANDARD")) {
            shippingCost = 40.0;
        } else if (shippingType.equals("EXPRESS")) {
            shippingCost = 150.0;
        } else if (shippingType.equals("PICKUP")) {
            shippingCost = 0.0;
        } else {
            throw new IllegalArgumentException("Unknown shipping type: " + shippingType);
        }

        double total = amount + shippingCost;
        System.out.println("Order " + orderId + " total: " + total);

        database.insert("INSERT INTO orders VALUES ('" + orderId + "', " + total + ")");
        System.out.println("Sending email: your order " + orderId + " is confirmed");
        System.out.println("AUDIT: order " + orderId + " placed at amount " + total);
    }
}

class MySqlDatabase {
    void insert(String sql) { System.out.println("MySQL: " + sql); }
}

interface Storage {
    void save(String id);
    void delete(String id);
    void archive(String id);
    void backup(String id);
}

class FileStorage implements Storage {
    @Override public void save(String id)    { System.out.println("Saved " + id + " to disk"); }
    @Override public void delete(String id)  { System.out.println("Deleted " + id + " from disk"); }
    @Override public void archive(String id) { throw new UnsupportedOperationException("Archive needs cold storage"); }
    @Override public void backup(String id)  { throw new UnsupportedOperationException("Backup needs cold storage"); }
}

class StandardShipping {
    void dispatch(String orderId)     { System.out.println("Dispatched " + orderId); }
    void trackParcel(String orderId)  { System.out.println("Tracking " + orderId); }
}

class InStorePickup extends StandardShipping {
    @Override void trackParcel(String orderId) {
        throw new UnsupportedOperationException("Nothing to track, customer collects it");
    }
}

// ============================================================
// HINTS — only after you've made your own list.
//
// SRP: OrderService prices, persists, emails and audits. Four actors, one file.
// OCP: the shipping-type if/else chain. Adding SAME_DAY means editing pricing logic.
// LSP: InStorePickup is not a StandardShipping — trackParcel is a lie.
// ISP: FileStorage is forced to implement archive/backup it cannot do.
// DIP: OrderService does `new MySqlDatabase()`. Try unit-testing that.
//
// Target shape (not the only right answer):
//   OrderService(OrderRepository, Notifier, AuditLog) + ShippingMethod interface
//   Storage split into Storage / Archivable
//   Shipping split into Dispatchable / Trackable
//
// Self-check to add to your Solution.java main:
//   assert total for ORD-1 is 1240.0 using a recording repository, no real DB.
// ============================================================
