package state.solution;

public class Shipped implements OrderState {

    @Override
    public OrderState deliver(Order order) {
        return new Delivered();
    }
}
