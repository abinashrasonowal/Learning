package naive;

import ladder.Race;

public class RaceMain {

    public static void main(String[] args) throws Exception {
        int distinct = Race.distinctInstances(Counter::getInstance, 32);
        System.out.println("naive: 32 threads saw " + distinct + " distinct instances");
        assert distinct > 1 : "race didn't fire — the bug is still there, the test was too fast";

        // Each caller may hold a different "singleton" — their counts silently diverge.
        System.out.println("naive: race fired, more than one singleton exists");
    }
}
