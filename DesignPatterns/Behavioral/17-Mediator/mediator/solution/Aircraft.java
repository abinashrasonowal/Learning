package mediator.solution;

// Knows one thing: its tower. Adding a 4th aircraft touches no existing aircraft.
public class Aircraft {

    public enum Status { AIRBORNE, HOLDING, ON_RUNWAY, LANDED }

    private final String callSign;
    private final Tower tower;
    private Status status = Status.AIRBORNE;

    public Aircraft(String callSign, Tower tower) {
        this.callSign = callSign;
        this.tower = tower;
    }

    public void requestLanding() {
        tower.requestLanding(this);
    }

    public void vacateRunway() {
        tower.runwayVacated(this);
        status = Status.LANDED;
    }

    // Called back by the tower — package-private, so only the tower side can drive them.
    void clearedToLand() {
        status = Status.ON_RUNWAY;
    }

    void hold() {
        status = Status.HOLDING;
    }

    public Status status() {
        return status;
    }

    public String callSign() {
        return callSign;
    }
}
