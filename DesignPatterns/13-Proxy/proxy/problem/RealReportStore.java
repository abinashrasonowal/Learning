package proxy.problem;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Connecting is the expensive part — and it happens in the constructor, at startup,
// whether or not anyone ever reads a report.
public class RealReportStore {
    private final Map<String, String> reports;

    public RealReportStore(Map<String, String> reports, AtomicInteger connections) {
        connections.incrementAndGet();
        this.reports = new HashMap<>(reports);
    }

    public String read(String id) {
        String report = reports.get(id);
        if (report == null) {
            throw new IllegalArgumentException("no report " + id);
        }
        return report;
    }

    public void delete(String id) {
        reports.remove(id);
    }

    public boolean exists(String id) {
        return reports.containsKey(id);
    }
}
