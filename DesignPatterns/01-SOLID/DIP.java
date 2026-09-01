// Dependency Inversion Principle — high-level policy depends on an abstraction,
// and the low-level detail implements it. The arrow points the other way.
// Run: java DIP.java

public class DIP {
    public static void main(String[] args) {
        System.out.println("--- BAD: high-level class wires up its own concretes ---");
        BadNotificationService bad = new BadNotificationService();
        bad.notifyByEmail("Your order has been shipped");
        bad.notifyBySMS("OTP 1234");
        // Adding push notifications = editing BadNotificationService (also breaks OCP).
        // Testing it = actually sending email. No seam to swap anything.

        System.out.println("\n--- GOOD: channel injected through an interface ---");
        new NotificationService(new EmailService()).send("Your order has been shipped");
        new NotificationService(new SMSService()).send("OTP 1234");
        new NotificationService(new PushService()).send("Flash sale starts now");

        // The seam pays for itself in tests: no real channel needed.
        RecordingChannel spy = new RecordingChannel();
        new NotificationService(spy).send("hello");
        assert "hello".equals(spy.lastMessage) : "channel did not receive the message";
        System.out.println("check passed: spy captured -> " + spy.lastMessage);
    }
}

// ---------- BAD ----------
class BadEmailService {
    void sendEmail(String message) { System.out.println("Sending email: " + message); }
}

class BadSMSService {
    void sendSMS(String message) { System.out.println("Sending SMS: " + message); }
}

class BadNotificationService {
    private final BadEmailService emailService;
    private final BadSMSService smsService;

    BadNotificationService() {
        this.emailService = new BadEmailService(); // welded to the concrete detail
        this.smsService = new BadSMSService();
    }

    void notifyByEmail(String msg) { emailService.sendEmail(msg); }
    void notifyBySMS(String msg)   { smsService.sendSMS(msg); }
}

// ---------- GOOD ----------
// The abstraction belongs to the high-level module; details conform to it.
interface NotificationChannel {
    void send(String message);
}

class NotificationService {
    private final NotificationChannel channel;

    NotificationService(NotificationChannel channel) { this.channel = channel; }

    void send(String message) { channel.send(message); }
}

class EmailService implements NotificationChannel {
    @Override public void send(String msg) { System.out.println("Sending email: " + msg); }
}

class SMSService implements NotificationChannel {
    @Override public void send(String msg) { System.out.println("Sending SMS: " + msg); }
}

// Added with zero edits to NotificationService — DIP buys you OCP for free.
class PushService implements NotificationChannel {
    @Override public void send(String msg) { System.out.println("Sending push: " + msg); }
}

// Test double — only possible because the dependency is an abstraction.
class RecordingChannel implements NotificationChannel {
    String lastMessage;
    @Override public void send(String msg) { this.lastMessage = msg; }
}
