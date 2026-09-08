package dip.problem;

public class NotificationService {
    private final EmailService emailService;

    public NotificationService() {
        // high-level policy reaching down and building a low-level detail.
        // no caller can hand us anything else — not an SMS channel, not a fake.
        this.emailService = new EmailService("smtp.company.com");
    }

    public void notifyUser(String user, String event) {
        emailService.sendEmail(user, "your " + event + " is ready");
    }
}
