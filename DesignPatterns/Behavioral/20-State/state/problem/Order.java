package state.problem;

// Every action is a switch over every status. A new status (RETURNED) means editing all
// four switches — and a missed case in a switch statement compiles fine and does nothing.
public class Order {
    private final int amount;
    private int refunded;
    private OrderStatus status = OrderStatus.CREATED;

    public Order(int amount) {
        this.amount = amount;
    }

    public void pay() {
        switch (status) {
            case CREATED -> status = OrderStatus.PAID;
            case PAID, SHIPPED, DELIVERED -> throw new IllegalStateException("already paid");
            case CANCELLED -> throw new IllegalStateException("order is cancelled");
        }
    }

    public void ship() {
        switch (status) {
            case PAID -> status = OrderStatus.SHIPPED;
            case CREATED -> throw new IllegalStateException("not paid yet");
            case SHIPPED, DELIVERED -> throw new IllegalStateException("already shipped");
            case CANCELLED -> throw new IllegalStateException("order is cancelled");
        }
    }

    public void deliver() {
        switch (status) {
            case SHIPPED -> status = OrderStatus.DELIVERED;
            case CREATED, PAID -> throw new IllegalStateException("not shipped yet");
            case DELIVERED -> throw new IllegalStateException("already delivered");
            // CANCELLED forgotten — compiles, silently does nothing
        }
    }

    public void cancel() {
        switch (status) {
            case CREATED -> status = OrderStatus.CANCELLED;
            case PAID -> {
                refunded = amount;
                status = OrderStatus.CANCELLED;
            }
            case SHIPPED, DELIVERED -> throw new IllegalStateException("too late to cancel");
            case CANCELLED -> throw new IllegalStateException("already cancelled");
        }
    }

    public OrderStatus status() {
        return status;
    }

    public int refunded() {
        return refunded;
    }
}
