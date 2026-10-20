package synced;

import ladder.Race;

// Correct. Cost: every read takes the lock forever, to guard a write that happens once.
public class Counter {
    private static Counter instance;
    private int count;

    private Counter() {
        Race.slowConstruction();
    }

    public static synchronized Counter getInstance() {
        if (instance == null) {
            instance = new Counter();
        }
        return instance;
    }

    public synchronized void increment() {
        count++;
    }

    public synchronized int count() {
        return count;
    }
}
