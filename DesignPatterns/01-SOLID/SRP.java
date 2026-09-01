// Single Responsibility Principle — a class should have ONE reason to change.
// Run: java SRP.java

public class SRP {
    public static void main(String[] args) {
        System.out.println("--- BAD: one class, three actors ---");
        BadInvoice bad = new BadInvoice(250.0);
        bad.generateInvoice();
        bad.saveToDatabase();      // schema change -> edits Invoice
        bad.sendEmailNotification(); // SMTP change -> edits Invoice

        System.out.println("\n--- GOOD: split by reason to change ---");
        Invoice invoice = new Invoice(250.0);
        invoice.generateInvoice();
        new InvoiceRepository().save(invoice);
        new EmailService().send("Invoice for " + invoice.getAmount());
    }
}

// ---------- BAD ----------
// Three unrelated actors (accounting, DBA, comms) all edit this one file.
class BadInvoice {
    private final double amount;

    BadInvoice(double amount) { this.amount = amount; }

    void generateInvoice()      { System.out.println("Invoice generated for " + amount); }
    void saveToDatabase()       { System.out.println("Saving invoice to database"); }
    void sendEmailNotification(){ System.out.println("Sending email for invoice"); }
}

// ---------- GOOD ----------
// Business rules only. Changes when the accountant's rules change.
class Invoice {
    private final double amount;

    Invoice(double amount) { this.amount = amount; }

    double getAmount() { return amount; }

    void generateInvoice() { System.out.println("Invoice generated for " + amount); }
}

// Persistence only. Changes when the schema changes.
class InvoiceRepository {
    void save(Invoice invoice) {
        System.out.println("Saving invoice of " + invoice.getAmount() + " to database");
    }
}

// Notification only. Changes when the mail provider changes.
class EmailService {
    void send(String message) { System.out.println("Sending email: " + message); }
}
