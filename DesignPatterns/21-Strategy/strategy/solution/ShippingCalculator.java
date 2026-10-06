package strategy.solution;

import java.util.Objects;

// The context: owns the workflow around pricing, never knows which carrier it holds.
public class ShippingCalculator {
    private final ShippingStrategy strategy;

    public ShippingCalculator(ShippingStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public double cost(Parcel parcel) {
        if (parcel.weightKg() <= 0) {
            throw new IllegalArgumentException("weight must be positive, got " + parcel.weightKg());
        }
        return strategy.cost(parcel);
    }
}
