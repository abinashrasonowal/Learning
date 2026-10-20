package mediator.solution;

// The mediator. Aircraft only ever talk to this, never to each other.
public interface Tower {
    void register(Aircraft aircraft);

    void requestLanding(Aircraft aircraft);

    void runwayVacated(Aircraft aircraft);
}
