package holder;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import ladder.Race;

// The default. Holder isn't initialised until getInstance() first touches it, and the JVM
// runs class init exactly once — lazy and thread-safe with no lock in our code.
public class Counter implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final AtomicInteger constructed = new AtomicInteger();
    private int count;

    private Counter() {
        Race.slowConstruction();
        constructed.incrementAndGet();
    }

    private static final class Holder {
        static final Counter INSTANCE = new Counter();
    }

    public static Counter getInstance() {
        return Holder.INSTANCE;
    }

    // Touches Counter but not Holder — used to prove the instance is still unbuilt.
    public static int constructedSoFar() {
        return constructed.get();
    }

    // Without this, deserializing makes a second Counter. Swap the copy for the real one.
    @Serial
    private Object readResolve() {
        return getInstance();
    }

    public synchronized void increment() {
        count++;
    }

    public synchronized int count() {
        return count;
    }
}
