package dip.solution;

public class NotificationService {
    private final NotificationChannel channel;
    private int notified;

    // no `new` in here — the caller decides which detail we talk to
    public NotificationService(NotificationChannel channel) {
        this.channel = channel;
    }

    public void notifyUser(String user, String event) {
        notified++;
        channel.send(user, "your " + event + " is ready (notification " + notified + ")");
    }

    public int notified() {
        return notified;
    }
}
