package srp.solution;

// ONE reason to change: the accountant changes the pricing rules.
// Knows nothing about databases, email, or how it gets displayed.
public class Invoice {
    private static final double TAX_RATE = 0.18;

    private final String id;
    private final double amount;

    public Invoice(String id, double amount){
        this.id = id;
        this.amount = amount;
    }

    public String getId(){
        return id;
    }

    public double getAmount(){
        return amount;
    }

    // Pricing logic stays here. Extracting this into a "TotalCalculator" would be
    // over-splitting: it changes for the same reason the rest of Invoice does.
    public double calculateTotal(){
        return amount + amount * TAX_RATE;
    }
}
