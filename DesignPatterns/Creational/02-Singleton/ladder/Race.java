package ladder;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

// Starts N threads on a latch so they all hit getInstance() together, then counts
// how many DISTINCT objects came back — by identity, not by equals() or a log line.
public final class Race {

    private Race() {
    }

    public static int distinctInstances(Supplier<?> getInstance, int threads) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return getInstance.get();
                }));
            }
            start.countDown();

            Set<Object> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Future<Object> result : results) {
                distinct.add(result.get());
            }
            return distinct.size();
        } finally {
            pool.shutdown();
        }
    }

    // Real work in the constructor widens the race window. An empty constructor is
    // effectively atomic, so the naive race would hide — the bug wouldn't be gone.
    public static void slowConstruction() {
        long until = System.nanoTime() + 20_000_000;   // ~20 ms of busy work
        while (System.nanoTime() < until) {
            Thread.onSpinWait();
        }
    }
}
