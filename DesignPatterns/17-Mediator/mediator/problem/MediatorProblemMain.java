package mediator.problem;

public class MediatorProblemMain {

    public static void main(String[] args) {
        Aircraft a = new Aircraft("AI-101");
        Aircraft b = new Aircraft("6E-202");
        Aircraft c = new Aircraft("UK-303");

        // 3 aircraft need 3 two-way links. Wire two of them, forget A <-> C.
        link(a, b);
        link(b, c);

        assert a.requestLanding();
        assert !b.requestLanding();     // B knows A, so it waits — correct
        assert c.requestLanding();      // C never heard of A — lands anyway

        assert a.isOnRunway() && c.isOnRunway() : "two aircraft on one runway";
        System.out.println("collision: " + a.callSign() + " and " + c.callSign() + " both on the runway");

        // A leaves; B isn't told and stays airborne until it polls again.
        a.vacateRunway();
        c.vacateRunway();
        assert !b.isOnRunway();
        assert b.requestLanding();

        // A 4th aircraft = 3 more links, each in two directions, each easy to forget.
        System.out.println("problem: all asserts passed");
    }

    private static void link(Aircraft x, Aircraft y) {
        x.addPeer(y);
        y.addPeer(x);
    }
}
