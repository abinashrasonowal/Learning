package attack;

import java.lang.reflect.Constructor;

public class ReflectionAttack {

    public static void main(String[] args) throws Exception {
        // Holder: the private constructor is only private to the compiler.
        Constructor<holder.Counter> ctor = holder.Counter.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        holder.Counter forged = ctor.newInstance();
        assert forged != holder.Counter.getInstance();
        System.out.println("reflection: broke holder, second instance created");

        // Enum: the JVM refuses outright. Its hidden constructor is (String name, int ordinal).
        Constructor<enumsingleton.Counter> enumCtor =
                enumsingleton.Counter.class.getDeclaredConstructor(String.class, int.class);
        enumCtor.setAccessible(true);
        try {
            enumCtor.newInstance("FORGED", 1);
            assert false : "expected the enum to refuse";
        } catch (IllegalArgumentException expected) {
            System.out.println("reflection: enum refused — " + expected.getMessage());
        }

        System.out.println("reflection attack: all asserts passed");
    }
}
