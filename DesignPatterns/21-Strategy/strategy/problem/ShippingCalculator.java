package strategy.problem;

public class ShippingCalculator {

    // Every new carrier means editing this method — and re-testing the ones that already worked.
    public double cost(String carrier, Parcel p) {
        if (carrier.equals("Standard")) {
            return 5 * p.weightKg() + 0.5 * p.distanceKm();
        } else if (carrier.equals("Express")) {
            double standard = 5 * p.weightKg() + 0.5 * p.distanceKm();   // duplicated from above
            return standard * 1.5 + 50;
        } else if (carrier.equals("SameDay")) {
            if (p.distanceKm() > 30) {
                throw new IllegalArgumentException("SameDay only within 30 km, got " + p.distanceKm());
            }
            return 200 + 10 * p.weightKg();
        } else {
            throw new IllegalArgumentException("Unknown carrier: " + carrier);
        }
    }
}
