package proxy.solution;

// Same as problem/'s — still no check. It doesn't need one: it's given a store for this user.
public class ArchivePage {
    private final ReportStore store;

    public ArchivePage(ReportStore store) {
        this.store = store;
    }

    public void purge(String id) {
        store.delete(id);
    }
}
