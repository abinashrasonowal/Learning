package strategy.solution;

// Takes only what EVERY carrier needs. Carrier-specific numbers live in each strategy's fields.
public interface ShippingStrategy {
    double cost(Parcel parcel);
}
