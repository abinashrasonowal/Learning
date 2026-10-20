package abstractfactory.problem;

public class StripeCharger {
    private int next = 1;

    public String charge(int amount) {
        return "ch_" + next++;
    }
}
