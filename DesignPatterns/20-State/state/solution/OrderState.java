package state.solution;

// Each action returns the next state. Illegal by default: a state overrides only the
// actions that are legal from it, so a forgotten case fails loudly instead of silently.
public interface OrderState {

    default OrderState pay(Order order) {
        throw illegal("pay");
    }

    default OrderState ship(Order order) {
        throw illegal("ship");
    }

    default OrderState deliver(Order order) {
        throw illegal("deliver");
    }

    default OrderState cancel(Order order) {
        throw illegal("cancel");
    }

    default String name() {
        return getClass().getSimpleName();
    }

    private IllegalStateException illegal(String action) {
        return new IllegalStateException("cannot " + action + " a " + name() + " order");
    }
}
