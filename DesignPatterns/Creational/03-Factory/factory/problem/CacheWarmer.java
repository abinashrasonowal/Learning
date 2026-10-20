package factory.problem;

public class CacheWarmer {

    // Copy of BackupJob.open() — minus the bucket validation, which never made it over.
    public Storage open(String uri) {
        if (uri.equals("mem:")) {
            return new MemoryStorage();
        } else if (uri.startsWith("s3://")) {
            return new S3Storage(uri.substring("s3://".length()));
        }
        throw new IllegalArgumentException("unknown storage: " + uri);
    }
}
