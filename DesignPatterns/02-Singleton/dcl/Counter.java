package dcl;

import java.io.Serializable;

import ladder.Race;

// Double-checked locking. volatile stops another thread seeing a non-null reference to a
// half-constructed object (reordering, not staleness). Remove it and the race test still
// passes — that's what makes the bug dangerous.
//
// Serializable with NO readResolve on purpose: attack.SerializationAttack breaks this one.
public class Counter implements Serializable {
    private static final long serialVersionUID = 1L;
    private static volatile Counter instance;
    private int count;

    private Counter() {
        Race.slowConstruction();
    }

    public static Counter getInstance() {
        Counter local = instance;               // one volatile read on the fast path
        if (local == null) {
            synchronized (Counter.class) {
                local = instance;
                if (local == null) {
                    local = new Counter();
                    instance = local;
                }
            }
        }
        return local;
    }

    public synchronized void increment() {
        count++;
    }

    public synchronized int count() {
        return count;
    }
}
