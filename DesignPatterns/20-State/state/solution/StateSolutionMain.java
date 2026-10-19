package state.solution;

public class StateSolutionMain {

    public static void main(String[] args) {
        // Same scenarios as problem/ — behaviour preserved.
        Order happy = new Order(500);
        happy.pay();
        happy.ship();
        happy.deliver();
        assert happy.status().equals("Delivered");

        Order refundedOrder = new Order(500);
        refundedOrder.pay();
        refundedOrder.cancel();
        assert refundedOrder.status().equals("Cancelled");
        assert refundedOrder.refunded() == 500;

        // Cancel before paying: no refund, because only Paid.cancel() refunds.
        Order unpaid = new Order(500);
        unpaid.cancel();
        assert unpaid.refunded() == 0;

        Order late = new Order(500);
        late.pay();
        late.ship();
        expectRejected(late::cancel);
        assert late.status().equals("Shipped");     // a rejected action leaves the state alone

        // The problem's silent bug, fixed by the default: Cancelled never overrode deliver().
        Order cancelled = new Order(500);
        cancelled.cancel();
        expectRejected(cancelled::deliver);

        expectRejected(happy::pay);
        expectRejected(new Order(500)::ship);

        System.out.println("solution: all asserts passed");
    }

    private static void expectRejected(Runnable action) {
        try {
            action.run();
            assert false : "expected IllegalStateException";
        } catch (IllegalStateException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }
    }
}
