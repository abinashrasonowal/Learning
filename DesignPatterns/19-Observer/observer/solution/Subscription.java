package observer.solution;

// AutoCloseable without the checked exception, so try-with-resources needs no catch.
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
