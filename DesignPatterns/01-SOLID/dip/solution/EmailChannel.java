package dip.solution;

public class EmailChannel implements NotificationChannel {
    private final String smtpHost;
    private int sent;

    public EmailChannel(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    @Override
    public void send(String user, String message) {
        sent++;
        System.out.println("[" + smtpHost + "] email #" + sent + " to " + user + ": " + message);
    }
}
