package state.solution;

public class Paid implements OrderState {

    @Override
    public OrderState ship(Order order) {
        return new Shipped();
    }

    // The refund rule lives in the one state where it applies.
    @Override
    public OrderState cancel(Order order) {
        order.refund();
        return new Cancelled();
    }
}
