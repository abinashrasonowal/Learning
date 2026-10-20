package strategy.solution;

public class SameDay implements ShippingStrategy {
    private final double baseFee;
    private final double perKg;
    private final double maxDistanceKm;

    public SameDay(double baseFee, double perKg, double maxDistanceKm) {
        this.baseFee = baseFee;
        this.perKg = perKg;
        this.maxDistanceKm = maxDistanceKm;
    }

    @Override
    public double cost(Parcel parcel) {
        if (parcel.distanceKm() > maxDistanceKm) {
            throw new IllegalArgumentException(
                    "SameDay only within " + maxDistanceKm + " km, got " + parcel.distanceKm());
        }
        return baseFee + perKg * parcel.weightKg();
    }
}
