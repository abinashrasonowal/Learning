package proxy.solution;

import java.util.function.Supplier;

// Virtual proxy: looks like a ReportStore, creates the real one on first use.
// synchronized so two threads can't both connect.
public class LazyReportStore implements ReportStore {
    private final Supplier<ReportStore> connect;
    private ReportStore real;

    public LazyReportStore(Supplier<ReportStore> connect) {
        this.connect = connect;
    }

    private synchronized ReportStore real() {
        if (real == null) {
            real = connect.get();
        }
        return real;
    }

    @Override
    public String read(String id) {
        return real().read(id);
    }

    @Override
    public void delete(String id) {
        real().delete(id);
    }

    @Override
    public boolean exists(String id) {
        return real().exists(id);
    }
}
