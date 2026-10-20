package factory.problem;

public class FactoryProblemMain {

    public static void main(String[] args) {
        BackupJob backup = new BackupJob();
        CacheWarmer warmer = new CacheWarmer();

        assert backup.open("mem:").describe().equals("memory");
        assert backup.open("s3://daily-backups").describe().equals("s3:daily-backups");
        assert warmer.open("s3://daily-backups").describe().equals("s3:daily-backups");

        // Same URI, different answers — the creation logic was copied and drifted.
        String bad = "s3://Bad_Bucket";
        try {
            backup.open(bad);
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("backup rejected: " + expected.getMessage());
        }
        assert warmer.open(bad).describe().equals("s3:Bad_Bucket");
        System.out.println("cache warmer accepted an invalid bucket");

        // Adding "file:" storage = editing BackupJob AND CacheWarmer (and whoever copied them next).
        System.out.println("problem: all asserts passed");
    }
}
