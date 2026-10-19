package mediator.solution;

import mediator.solution.Aircraft.Status;

public class MediatorSolutionMain {

    public static void main(String[] args) {
        ControlTower tower = new ControlTower();
        Aircraft a = new Aircraft("AI-101", tower);
        Aircraft b = new Aircraft("6E-202", tower);
        Aircraft c = new Aircraft("UK-303", tower);

        // n registrations instead of n(n-1)/2 links.
        tower.register(a);
        tower.register(b);
        tower.register(c);

        a.requestLanding();
        b.requestLanding();
        c.requestLanding();

        // The scenario that collided in problem/ — now only one is on the runway.
        assert a.status() == Status.ON_RUNWAY;
        assert b.status() == Status.HOLDING;
        assert c.status() == Status.HOLDING;
        assert tower.holdingCount() == 2;

        // Vacating hands the runway to the next in line — nobody polls.
        a.vacateRunway();
        assert a.status() == Status.LANDED;
        assert b.status() == Status.ON_RUNWAY;
        assert c.status() == Status.HOLDING;

        // Only the aircraft on the runway can vacate it.
        expectRejected(c::vacateRunway);

        b.vacateRunway();
        assert c.status() == Status.ON_RUNWAY;
        assert tower.holdingCount() == 0;

        // Forgot to register? The tower says so, instead of a silent collision.
        Aircraft rogue = new Aircraft("XX-999", tower);
        expectRejected(rogue::requestLanding);

        // A 4th aircraft: one register call, zero edits to a, b or c.
        Aircraft d = new Aircraft("SG-404", tower);
        tower.register(d);
        d.requestLanding();
        assert d.status() == Status.HOLDING;
        c.vacateRunway();
        assert d.status() == Status.ON_RUNWAY;

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
