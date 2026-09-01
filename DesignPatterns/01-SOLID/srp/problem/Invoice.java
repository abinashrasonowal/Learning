package srp.problem;

public class Invoice {
    private static final double TAX_RATE = 0.18;

    private final double amount;

    Invoice(double amount){
        this.amount = amount;
    }

    public void generateInvoice(){
        double total = calculateTotal();
        saveInDB(total);
        sendEmail();
        System.out.println("invoice generated");
    }

    // Pricing rules — this one genuinely belongs to Invoice.
    public double calculateTotal(){
        return amount + amount * TAX_RATE;
    }

    // Persistence — changes when the schema changes. Wrong actor, wrong class.
    public void saveInDB(double total){
        System.out.println(total + " saved to db");
    }

    // Notification — changes when the mail provider changes. Wrong actor again.
    public void sendEmail(){
        System.out.println("email sent");
    }
}
