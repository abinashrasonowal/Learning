package dip.problem;

public class EmailService {
    private final String smtpHost;
    private int sent;

    public EmailService(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public void sendEmail(String to, String message) {
        sent++;
        System.out.println("[" + smtpHost + "] email #" + sent + " to " + to + ": " + message);
    }
}
