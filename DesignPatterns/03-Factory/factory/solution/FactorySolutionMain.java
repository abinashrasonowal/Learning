package factory.solution;

public class FactorySolutionMain {

    public static void main(String[] args) {
        StorageFactory factory = StorageFactory.withDefaults();

        // Same URIs as problem/ — same results.
        assert factory.create("mem:").describe().equals("memory");
        assert new BackupJob(factory.create("s3://daily-backups")).target().equals("s3:daily-backups");
        assert new CacheWarmer(factory.create("s3://daily-backups")).source().equals("s3:daily-backups");

        // The drift is gone: one creation path, so every caller rejects the same bad URI.
        expectRejected(() -> factory.create("s3://Bad_Bucket"));
        expectRejected(() -> factory.create("ftp://old-server"));
        expectRejected(() -> factory.create("file:relative/path"));

        // "file:" was added in solution/ without touching BackupJob or CacheWarmer.
        Storage shared = factory.create("file:/var/backups");
        new BackupJob(shared).backup("orders", "42 rows");
        assert new CacheWarmer(shared).warm("orders").equals("42 rows");

        // A 4th scheme from outside the factory: one register call, zero edits to any class.
        factory.register("gcs://", bucket -> new MemoryStorage() {
            @Override
            public String describe() {
                return "gcs:" + bucket;
            }
        });
        assert factory.create("gcs://archive").describe().equals("gcs:archive");

        System.out.println("solution: all asserts passed");
    }

    private static void expectRejected(Runnable action) {
        try {
            action.run();
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }
    }
}
