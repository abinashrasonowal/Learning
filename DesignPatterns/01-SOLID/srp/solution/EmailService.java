package srp.solution;

// ONE reason to change: the mail provider or the message template changes.
public class EmailService {
    public void send(String message){
        System.out.println("email sent: " + message);
    }
}
