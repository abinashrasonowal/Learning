package strategy.solution;

public class StrategySolutionMain {

    public static void main(String[] args) {
        Standard standard = new Standard(5, 0.5);

        ShippingRegistry registry = new ShippingRegistry();
        registry.register("Standard", standard);
        registry.register("Express", new Express(standard, 1.5, 50));
        registry.register("SameDay", new SameDay(200, 10, 30));

        Parcel parcel = new Parcel(4, 20);

        // Same numbers as problem/ — behaviour preserved.
        assert registry.calculatorFor("Standard").cost(parcel) == 30.0;
        assert registry.calculatorFor("Express").cost(parcel) == 95.0;
        assert registry.calculatorFor("SameDay").cost(parcel) == 240.0;

        // State matters: a pricier Standard changes the result without touching any class.
        assert new ShippingCalculator(new Standard(10, 1)).cost(parcel) == 60.0;

        // 4th carrier as a lambda — zero new files, zero edits to ShippingCalculator.
        registry.register("Pickup", p -> 0.0);
        assert registry.calculatorFor("Pickup").cost(parcel) == 0.0;

        expectRejected(() -> registry.calculatorFor("Drone"));
        expectRejected(() -> registry.calculatorFor("SameDay").cost(new Parcel(4, 45)));

        // Runtime swap: the carrier string is read in exactly one place — the registry.
        String[] userChoices = args.length > 0 ? args : new String[]{"Standard", "Express", "Pickup"};
        for (String choice : userChoices) {
            System.out.println(choice + " -> " + registry.calculatorFor(choice).cost(parcel));
        }

        System.out.println("solution: all asserts passed");
    }

    private static void expectRejected(Runnable action) {
        try {
            action.run();
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }
    }
}
