package ocp.solution;

public class CreditCard implements PaymentMethod {
    private final String cardNumber;

    public CreditCard(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @Override
    public void pay(double amount) {
        System.out.println("amount " + amount + " paid using credit card " + cardNumber);
    }
}
