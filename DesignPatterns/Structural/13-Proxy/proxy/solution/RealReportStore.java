package proxy.solution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// The real subject — unchanged in spirit from problem/, now behind an interface.
public class RealReportStore implements ReportStore {
    private final Map<String, String> reports;

    public RealReportStore(Map<String, String> reports, AtomicInteger connections) {
        connections.incrementAndGet();
        this.reports = new HashMap<>(reports);
    }

    @Override
    public String read(String id) {
        String report = reports.get(id);
        if (report == null) {
            throw new IllegalArgumentException("no report " + id);
        }
        return report;
    }

    @Override
    public void delete(String id) {
        reports.remove(id);
    }

    @Override
    public boolean exists(String id) {
        return reports.containsKey(id);
    }
}
