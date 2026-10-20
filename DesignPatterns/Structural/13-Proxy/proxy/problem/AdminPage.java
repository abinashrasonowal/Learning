package proxy.problem;

public class AdminPage {
    private final RealReportStore store;

    public AdminPage(RealReportStore store) {
        this.store = store;
    }

    public void deleteReport(User user, String id) {
        if (!user.isAdmin()) {
            throw new SecurityException(user.name() + " may not delete reports");
        }
        store.delete(id);
    }
}
