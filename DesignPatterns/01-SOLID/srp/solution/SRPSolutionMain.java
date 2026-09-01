package srp.solution;

// The driver coordinates the three collaborators. Deliberately NOT a fourth
// "InvoiceService" class — nothing needs one yet.
public class SRPSolutionMain {

    public static void main(String[] args){
        Invoice invoice = new Invoice("INV-1", 100);
        DatabaseRepository repository = new DatabaseRepository();
        EmailService emailService = new EmailService();

        System.out.println("invoice generated for " + invoice.calculateTotal());
        repository.save(invoice);
        emailService.send("your invoice " + invoice.getId() + " is ready");

        // Self-check: pricing is testable on its own now — no db, no mail server.
        assert invoice.calculateTotal() == 118.0 : "expected 118.0, got " + invoice.calculateTotal();
        System.out.println("check passed: total = " + invoice.calculateTotal());
    }
}
