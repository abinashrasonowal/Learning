package facade.solution;

import java.util.ArrayList;
import java.util.List;

public class Notifier {
    private final List<String> sent = new ArrayList<>();

    public void send(String to, String message) {
        sent.add(to + ": " + message);
    }

    public List<String> sent() {
        return sent;
    }
}
