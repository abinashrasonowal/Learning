package dip.solution;

// owned by the high-level module, phrased in its language ("send a notification"),
// not the detail's ("sendEmail"). The details implement this — arrow inverted.
public interface NotificationChannel {
    void send(String user, String message);
}
