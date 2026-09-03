package ocp.problem;

public class PaymentProcessor {
    public void processPayment(PaymentMethod method, double amount){
        if( method == PaymentMethod.CREDIT_CARD){
            payUsingCreditCard(amount);
        } else if (method == PaymentMethod.DEBIT_CARD) {
            payUsingDebitCard(amount);
        } else if ( method == PaymentMethod.UPI) {
            payUsingUPI(amount);
        }else {
             throw new IllegalArgumentException("payment method not supported");
        }
    }

    void payUsingCreditCard(double amount){
        System.out.println("amount " + amount + " paid using credit card" );
    }

    void payUsingDebitCard(double amount){
        System.out.println("amount " + amount + " paid using debit card" );
    }

    void payUsingUPI(double amount){
        System.out.println("amount " + amount + " paid using UPI" );
    }
}
