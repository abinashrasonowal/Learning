package abstractfactory.solution;

class StripeCharger implements Charger {
    private int next = 1;

    @Override
    public String charge(int amount) {
        return "ch_" + next++;
    }
}
