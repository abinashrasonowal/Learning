package abstractfactory.problem;

public class RazorpayCharger {
    private int next = 1;

    public String charge(int amount) {
        return "pay_" + next++;
    }
}
