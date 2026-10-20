package naive;

import ladder.Race;

// Lazy and unsynchronised — the course's AppSettings shape. Two threads can both see null.
public class Counter {
    private static Counter instance;
    private int count;

    private Counter() {
        Race.slowConstruction();
    }

    public static Counter getInstance() {
        if (instance == null) {
            instance = new Counter();
        }
        return instance;
    }

    public void increment() {
        count++;
    }

    public int count() {
        return count;
    }
}
