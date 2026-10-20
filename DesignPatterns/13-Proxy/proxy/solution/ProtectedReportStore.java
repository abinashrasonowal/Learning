package proxy.solution;

// Protection proxy: the permission rule moves out of every caller into the one object
// callers are handed. A page that forgets to check can't skip it — it never sees the real store.
public class ProtectedReportStore implements ReportStore {
    private final ReportStore inner;
    private final User user;

    public ProtectedReportStore(ReportStore inner, User user) {
        this.inner = inner;
        this.user = user;
    }

    @Override
    public String read(String id) {
        return inner.read(id);
    }

    @Override
    public void delete(String id) {
        if (!user.isAdmin()) {
            throw new SecurityException(user.name() + " may not delete reports");
        }
        inner.delete(id);
    }

    @Override
    public boolean exists(String id) {
        return inner.exists(id);
    }
}
