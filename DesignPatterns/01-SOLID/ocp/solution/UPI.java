package ocp.solution;

public class UPI implements PaymentMethod {
    private final String vpa;

    public UPI(String vpa) {
        this.vpa = vpa;
    }

    @Override
    public void pay(double amount) {
        System.out.println("amount " + amount + " paid using UPI " + vpa);
    }
}
