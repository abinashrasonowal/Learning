package ocp.solution;

import java.util.List;

public class OCPSolutionMain {
    public static void main(String[] args) {
        List<PaymentMethod> methods = List.of(
                new CreditCard("4111"),
                new DebitCard("5500"),
                new UPI("abi@upi"));

        for (PaymentMethod method : methods) {
            new PaymentProcessor(method).processPayment(100.0);
        }
    }
}
