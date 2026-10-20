package adapter.solution;

// Wraps the vendor and translates every difference in one place: enum -> pair string,
// the 10_000 scale, the -1 sentinel -> exception, and the inverse pairs it doesn't quote.
public class FxVendorAdapter implements ExchangeRates {
    private static final double SCALE = 10_000.0;
    private final FxVendorClient vendor;

    public FxVendorAdapter(FxVendorClient vendor) {
        this.vendor = vendor;
    }

    @Override
    public double rate(Currency from, Currency to) {
        if (from == to) {
            return 1.0;
        }
        if (from == Currency.USD) {
            return usdTo(to);
        }
        if (to == Currency.USD) {
            return 1.0 / usdTo(from);
        }
        return usdTo(to) / usdTo(from);     // cross rate via USD, e.g. INR -> EUR
    }

    private double usdTo(Currency to) {
        long scaled = vendor.getQuote("USD" + to.name());
        if (scaled < 0) {
            throw new IllegalArgumentException("vendor has no quote for USD" + to.name());
        }
        return scaled / SCALE;
    }
}
