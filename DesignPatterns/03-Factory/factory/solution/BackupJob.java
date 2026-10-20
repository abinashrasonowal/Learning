package factory.solution;

// Depends on the Storage interface only. Doesn't parse URIs, doesn't know S3 exists.
public class BackupJob {
    private final Storage storage;

    public BackupJob(Storage storage) {
        this.storage = storage;
    }

    public void backup(String key, String value) {
        storage.put(key, value);
    }

    public String target() {
        return storage.describe();
    }
}
