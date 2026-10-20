package adapter.problem;

// Business code that speaks the vendor's dialect: pair strings, the 10_000 scale, the -1
// sentinel. Every caller of the vendor repeats this, and this one already got it wrong.
public class PriceConverter {
    private final FxVendorClient vendor = new FxVendorClient();

    public double convert(double amount, String from, String to) {
        long scaled = vendor.getQuote(from + to);
        return amount * scaled / 10_000.0;      // forgot: -1 means "unknown", and no inverse pairs
    }
}
