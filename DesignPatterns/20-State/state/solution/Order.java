package state.solution;

// The context: no switch, no status enum. It forwards each action to the current state
// and keeps whatever state comes back.
public class Order {
    private final int amount;
    private int refunded;
    private OrderState state = new Created();

    public Order(int amount) {
        this.amount = amount;
    }

    public void pay() {
        state = state.pay(this);
    }

    public void ship() {
        state = state.ship(this);
    }

    public void deliver() {
        state = state.deliver(this);
    }

    public void cancel() {
        state = state.cancel(this);
    }

    // Package-private: only states may trigger side effects on the order.
    void refund() {
        refunded = amount;
    }

    public String status() {
        return state.name();
    }

    public int refunded() {
        return refunded;
    }
}
