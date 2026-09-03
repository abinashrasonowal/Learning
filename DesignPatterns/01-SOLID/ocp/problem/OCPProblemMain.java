package ocp.problem;

public class OCPProblemMain {
    public static void main(String[] args) {
        PaymentProcessor paymentProcessor = new PaymentProcessor();
        paymentProcessor.processPayment(PaymentMethod.UPI, 10.2);
    }
}
