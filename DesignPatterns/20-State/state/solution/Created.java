package state.solution;

public class Created implements OrderState {

    @Override
    public OrderState pay(Order order) {
        return new Paid();
    }

    @Override
    public OrderState cancel(Order order) {
        return new Cancelled();
    }
}
