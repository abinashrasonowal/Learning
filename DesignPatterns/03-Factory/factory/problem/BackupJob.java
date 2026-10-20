package factory.problem;

// Parses the URI and picks a class itself. So does CacheWarmer — two copies of the
// creation logic, and they have already drifted.
public class BackupJob {

    public Storage open(String uri) {
        if (uri.equals("mem:")) {
            return new MemoryStorage();
        } else if (uri.startsWith("s3://")) {
            String bucket = uri.substring("s3://".length());
            if (!bucket.matches("[a-z0-9.-]{3,63}")) {
                throw new IllegalArgumentException("invalid bucket name: " + bucket);
            }
            return new S3Storage(bucket);
        }
        throw new IllegalArgumentException("unknown storage: " + uri);
    }
}
