package strategy.problem;

public class StrategyProblemMain {

    public static void main(String[] args) {
        ShippingCalculator calculator = new ShippingCalculator();
        Parcel parcel = new Parcel(4, 20);

        assert calculator.cost("Standard", parcel) == 30.0;
        assert calculator.cost("Express", parcel) == 95.0;
        assert calculator.cost("SameDay", parcel) == 240.0;

        try {
            calculator.cost("Drone", parcel);
            assert false : "unknown carrier should throw";
        } catch (IllegalArgumentException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }

        // Adding "Pickup" = one more else-if inside ShippingCalculator.cost(). Edit tested code.
        System.out.println("problem: all asserts passed");
    }
}
