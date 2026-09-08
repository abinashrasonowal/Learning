package dip.problem;

public class DIPProblemMain {
    public static void main(String[] args) {
        NotificationService notificationService = new NotificationService();
        notificationService.notifyUser("abinash@example.com", "invoice");

        // Want to assert that exactly one notification went out?
        // You can't. There is no seam:
        //   - the EmailService is created inside the constructor, so nothing can replace it
        //   - notifyUser returns void and reports nothing back
        //   - the only evidence is a line on stdout and a real SMTP connection
        // Adding an SMS channel means editing this class — an OCP violation too.
    }
}
