package strategy.solution;

public class Express implements ShippingStrategy {
    private final ShippingStrategy base;   // reuses Standard instead of copy-pasting its formula
    private final double multiplier;
    private final double surcharge;

    public Express(ShippingStrategy base, double multiplier, double surcharge) {
        this.base = base;
        this.multiplier = multiplier;
        this.surcharge = surcharge;
    }

    @Override
    public double cost(Parcel parcel) {
        return base.cost(parcel) * multiplier + surcharge;
    }
}
