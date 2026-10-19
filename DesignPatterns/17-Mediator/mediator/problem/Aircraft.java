package mediator.problem;

import java.util.ArrayList;
import java.util.List;

// Every aircraft talks to every other aircraft directly. n aircraft = n(n-1)/2 links to wire,
// and the landing rule ("runway must be free") is re-implemented inside each one.
public class Aircraft {
    private final String callSign;
    private final List<Aircraft> peers = new ArrayList<>();
    private boolean onRunway;

    public Aircraft(String callSign) {
        this.callSign = callSign;
    }

    public void addPeer(Aircraft other) {
        peers.add(other);
    }

    // Only as safe as the peer list. Miss one link and two aircraft share the runway.
    public boolean requestLanding() {
        for (Aircraft peer : peers) {
            if (peer.isOnRunway()) {
                return false;
            }
        }
        onRunway = true;
        return true;
    }

    // Nobody waiting is told the runway is free — they have to keep asking.
    public void vacateRunway() {
        onRunway = false;
    }

    public boolean isOnRunway() {
        return onRunway;
    }

    public String callSign() {
        return callSign;
    }
}
