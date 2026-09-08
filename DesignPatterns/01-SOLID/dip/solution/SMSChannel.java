package dip.solution;

public class SMSChannel implements NotificationChannel {
    private final String gateway;

    public SMSChannel(String gateway) {
        this.gateway = gateway;
    }

    @Override
    public void send(String user, String message) {
        System.out.println("[" + gateway + "] sms to " + user + ": " + message);
    }
}
