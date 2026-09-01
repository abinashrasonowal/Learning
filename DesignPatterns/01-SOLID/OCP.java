// Open/Closed Principle — open for extension, closed for modification.
// Run: java OCP.java

public class OCP {
    public static void main(String[] args) {
        System.out.println("--- BAD: switch that grows with every new method ---");
        BadPaymentProcessor bad = new BadPaymentProcessor();
        bad.processPayment("CreditCard", 100);
        bad.processPayment("Paypal", 120);
        try {
            bad.processPayment("UPI", 50); // supporting UPI means EDITING this class
        } catch (IllegalArgumentException e) {
            System.out.println("Blew up: " + e.getMessage());
        }

        System.out.println("\n--- GOOD: new method = new class, zero edits ---");
        PaymentProcessor processor = new PaymentProcessor();
        processor.processPayment(new CreditCard(), 100);
        processor.processPayment(new PayPal(), 120);
        processor.processPayment(new UPI(), 50); // added without touching PaymentProcessor
    }
}

// ---------- BAD ----------
class BadPaymentProcessor {
    void processPayment(String paymentMethod, double amount) {
        if (paymentMethod.equals("CreditCard")) {
            System.out.println("Paying " + amount + " via Credit Card");
        } else if (paymentMethod.equals("DebitCard")) {
            System.out.println("Paying " + amount + " via Debit Card");
        } else if (paymentMethod.equals("Paypal")) {
            System.out.println("Paying " + amount + " via PayPal");
        } else {
            throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod);
        }
    }
}

// ---------- GOOD ----------
// The abstraction is the extension point. This file never changes again.
interface PaymentMethod {
    void pay(double amount);
}

class PaymentProcessor {
    void processPayment(PaymentMethod method, double amount) {
        method.pay(amount);
    }
}

class CreditCard implements PaymentMethod {
    @Override public void pay(double amount) { System.out.println("Paying " + amount + " via Credit Card"); }
}

class DebitCard implements PaymentMethod {
    @Override public void pay(double amount) { System.out.println("Paying " + amount + " via Debit Card"); }
}

class PayPal implements PaymentMethod {
    @Override public void pay(double amount) { System.out.println("Paying " + amount + " via PayPal"); }
}

// The whole point: adding this class required no edit anywhere else.
class UPI implements PaymentMethod {
    @Override public void pay(double amount) { System.out.println("Paying " + amount + " via UPI"); }
}
