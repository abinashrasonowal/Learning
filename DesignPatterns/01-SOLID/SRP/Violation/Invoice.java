package SRP.Violation;

public class Invoice {
    public static void main(String[] args) {
        generateInvoice();
    }

    static void generateInvoice(){
        int total = calculateTotal(5,6);
        saveInDB(total);
        sendEmail();
        System.out.println("invoice generated");
    }

    static int calculateTotal(int a, int  b){
        return a + b;
    }

    static void saveInDB(int total){
        System.out.println(total + " saved to db");
    }

    static void sendEmail(){
        System.out.println("email sent");
    }
}
