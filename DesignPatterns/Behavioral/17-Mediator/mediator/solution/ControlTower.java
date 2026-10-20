package mediator.solution;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

// The coordination rule lives here, once: one aircraft on the runway, the rest hold in order.
public class ControlTower implements Tower {
    private final Set<Aircraft> registered = new HashSet<>();
    private final Deque<Aircraft> holding = new ArrayDeque<>();
    private Aircraft onRunway;

    @Override
    public void register(Aircraft aircraft) {
        registered.add(aircraft);
    }

    @Override
    public void requestLanding(Aircraft aircraft) {
        if (!registered.contains(aircraft)) {
            throw new IllegalStateException(aircraft.callSign() + " is not registered with this tower");
        }
        if (onRunway == null) {
            grant(aircraft);
        } else {
            holding.addLast(aircraft);
            aircraft.hold();
        }
    }

    @Override
    public void runwayVacated(Aircraft aircraft) {
        if (aircraft != onRunway) {
            throw new IllegalStateException(aircraft.callSign() + " is not on the runway");
        }
        onRunway = null;
        Aircraft next = holding.pollFirst();
        if (next != null) {
            grant(next);
        }
    }

    public int holdingCount() {
        return holding.size();
    }

    private void grant(Aircraft aircraft) {
        onRunway = aircraft;
        aircraft.clearedToLand();
    }
}
