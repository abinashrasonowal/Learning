package ocp.solution;

public class DebitCard implements PaymentMethod {
    private final String cardNumber;

    public DebitCard(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @Override
    public void pay(double amount) {
        System.out.println("amount " + amount + " paid using debit card " + cardNumber);
    }
}
