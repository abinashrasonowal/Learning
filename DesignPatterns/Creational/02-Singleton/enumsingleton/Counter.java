package enumsingleton;

// Reflection and serialization both refuse to make a second enum constant —
// zero defensive code needed. Cost: eager, can't extend a class, no constructor args.
public enum Counter {
    INSTANCE;

    private int count;

    public synchronized void increment() {
        count++;
    }

    public synchronized int count() {
        return count;
    }
}
