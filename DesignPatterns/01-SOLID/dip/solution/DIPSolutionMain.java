package dip.solution;

public class DIPSolutionMain {
    public static void main(String[] args) {
        // same policy, two different details — NotificationService never changed
        new NotificationService(new EmailChannel("smtp.company.com"))
                .notifyUser("abinash@example.com", "invoice");
        new NotificationService(new SMSChannel("twilio"))
                .notifyUser("+91-99999-00000", "invoice");

        // and now the policy is testable without sending anything
        RecordingChannel recording = new RecordingChannel();
        NotificationService service = new NotificationService(recording);

        service.notifyUser("abinash@example.com", "invoice");
        assert recording.count() == 1 : "expected 1 send, got " + recording.count();

        service.notifyUser("abinash@example.com", "receipt");
        assert recording.count() == 2 : "expected 2 sends, got " + recording.count();
        assert service.notified() == 2 : "service lost count";
        assert recording.last().contains("receipt") : "wrong message: " + recording.last();

        System.out.println("recorded " + recording.count() + " sends, last: " + recording.last());
    }
}
