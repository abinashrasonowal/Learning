package proxy.problem;

// Another caller that deletes — and the permission check didn't come with it.
public class ArchivePage {
    private final RealReportStore store;

    public ArchivePage(RealReportStore store) {
        this.store = store;
    }

    public void purge(User user, String id) {
        store.delete(id);
    }
}
