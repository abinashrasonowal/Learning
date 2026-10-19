package state.problem;

public class StateProblemMain {

    public static void main(String[] args) {
        Order happy = new Order(500);
        happy.pay();
        happy.ship();
        happy.deliver();
        assert happy.status() == OrderStatus.DELIVERED;

        Order refundedOrder = new Order(500);
        refundedOrder.pay();
        refundedOrder.cancel();
        assert refundedOrder.status() == OrderStatus.CANCELLED;
        assert refundedOrder.refunded() == 500;

        Order late = new Order(500);
        late.pay();
        late.ship();
        try {
            late.cancel();
            assert false : "expected IllegalStateException";
        } catch (IllegalStateException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }

        // The bug: delivering a cancelled order should fail, but the missing case swallows it.
        Order cancelled = new Order(500);
        cancelled.cancel();
        cancelled.deliver();
        assert cancelled.status() == OrderStatus.CANCELLED;
        System.out.println("bug: deliver() on a cancelled order silently did nothing");

        System.out.println("problem: all asserts passed");
    }
}
