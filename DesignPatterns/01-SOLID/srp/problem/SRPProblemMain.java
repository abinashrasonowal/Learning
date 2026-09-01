package srp.problem;

public class SRPProblemMain {

    public static void main(String[] args){
        Invoice invoice = new Invoice(100);
        invoice.generateInvoice();
        invoice.saveInDB(invoice.calculateTotal());
        invoice.sendEmail();
    }
}
