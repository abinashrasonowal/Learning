package proxy.problem;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyProblemMain {

    public static void main(String[] args) {
        AtomicInteger connections = new AtomicInteger();
        RealReportStore store = new RealReportStore(Map.of("q3", "revenue up", "q4", "revenue flat"), connections);

        // Paid for a connection before anyone asked for a report.
        assert connections.get() == 1;
        System.out.println("connected at startup, 0 reads so far");

        User viewer = new User("ravi", "viewer");
        try {
            new AdminPage(store).deleteReport(viewer, "q3");
            assert false : "expected SecurityException";
        } catch (SecurityException expected) {
            System.out.println("admin page: " + expected.getMessage());
        }

        // The check lives in callers, so a caller that forgets it is a hole.
        new ArchivePage(store).purge(viewer, "q4");
        assert !store.exists("q4");
        System.out.println("archive page let a viewer delete q4");

        System.out.println("problem: all asserts passed");
    }
}
