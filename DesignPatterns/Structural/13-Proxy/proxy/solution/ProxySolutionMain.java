package proxy.solution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxySolutionMain {

    public static void main(String[] args) {
        AtomicInteger connections = new AtomicInteger();
        Map<String, String> data = Map.of("q3", "revenue up", "q4", "revenue flat");

        // Virtual proxy: nothing connects until the first real call.
        ReportStore store = new LazyReportStore(() -> new RealReportStore(data, connections));
        assert connections.get() == 0;
        assert store.read("q3").equals("revenue up");
        assert connections.get() == 1;
        store.read("q4");
        assert connections.get() == 1;     // connected once, reused

        // Protection proxy: each user is handed a store that enforces their role.
        User viewer = new User("ravi", "viewer");
        User admin = new User("meera", "admin");
        ReportStore asViewer = new ProtectedReportStore(store, viewer);
        ReportStore asAdmin = new ProtectedReportStore(store, admin);

        assert asViewer.read("q3").equals("revenue up");

        // The ArchivePage that let a viewer delete in problem/ — same code, now refused.
        try {
            new ArchivePage(asViewer).purge("q4");
            assert false : "expected SecurityException";
        } catch (SecurityException expected) {
            System.out.println("refused: " + expected.getMessage());
        }
        assert store.exists("q4");

        new ArchivePage(asAdmin).purge("q4");
        assert !store.exists("q4");

        // Dynamic proxy around any interface — no hand-written class for this proxy.
        Map<String, Integer> counts = new HashMap<>();
        ReportStore counted = CallCounter.counting(ReportStore.class, asViewer, counts);
        counted.read("q3");
        counted.read("q3");
        try {
            counted.delete("q3");
            assert false : "expected SecurityException";
        } catch (SecurityException expected) {
            System.out.println("refused through dynamic proxy: " + expected.getMessage());
        }
        assert counts.get("read") == 2 && counts.get("delete") == 1;

        System.out.println("solution: all asserts passed");
    }
}
