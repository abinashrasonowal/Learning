package abstractfactory.solution;

class RazorpayCharger implements Charger {
    private int next = 1;

    @Override
    public String charge(int amount) {
        return "pay_" + next++;
    }
}
