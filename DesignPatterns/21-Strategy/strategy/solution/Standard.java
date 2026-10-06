package strategy.solution;

public class Standard implements ShippingStrategy {
    private final double perKg;
    private final double perKm;

    public Standard(double perKg, double perKm) {
        this.perKg = perKg;
        this.perKm = perKm;
    }

    @Override
    public double cost(Parcel parcel) {
        return perKg * parcel.weightKg() + perKm * parcel.distanceKm();
    }
}
