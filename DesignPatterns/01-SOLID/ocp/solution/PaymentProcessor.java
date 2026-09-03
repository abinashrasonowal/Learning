package ocp.solution;

public class PaymentProcessor {
    private final PaymentMethod method;

    public PaymentProcessor(PaymentMethod method) {
        this.method = method;
    }

    public void processPayment(double amount){
        method.pay(amount);
    }
}
