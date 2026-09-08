package dip.solution;

import java.util.ArrayList;
import java.util.List;

// the test double the problem package had no way to accept
public class RecordingChannel implements NotificationChannel {
    private final List<String> sends = new ArrayList<>();

    @Override
    public void send(String user, String message) {
        sends.add(user + " | " + message);
    }

    public int count() {
        return sends.size();
    }

    public String last() {
        return sends.get(sends.size() - 1);
    }
}
