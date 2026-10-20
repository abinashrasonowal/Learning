package ladder;

public class SingletonMain {

    public static void main(String[] args) throws Exception {
        // Holder laziness: touching Counter's statics doesn't build the instance.
        assert holder.Counter.constructedSoFar() == 0;

        // Every rung above naive survives the same race that broke naive.
        assert Race.distinctInstances(synced.Counter::getInstance, 32) == 1;
        assert Race.distinctInstances(dcl.Counter::getInstance, 32) == 1;
        assert Race.distinctInstances(holder.Counter::getInstance, 32) == 1;
        assert Race.distinctInstances(() -> enumsingleton.Counter.INSTANCE, 32) == 1;

        assert holder.Counter.constructedSoFar() == 1 : "built on first getInstance(), exactly once";

        // Real state: one instance means one count, wherever it's reached from.
        holder.Counter a = holder.Counter.getInstance();
        holder.Counter b = holder.Counter.getInstance();
        a.increment();
        b.increment();
        assert a == b && a.count() == 2;

        System.out.println("ladder: synced, dcl, holder, enum each yield 1 instance under the race");
        System.out.println("ladder: all asserts passed");
    }
}
