package adapter.solution;

public class AdapterSolutionMain {

    public static void main(String[] args) {
        FxVendorClient vendor = new FxVendorClient();
        PriceConverter converter = new PriceConverter(new FxVendorAdapter(vendor));

        // Same direct pair as problem/.
        assert close(converter.convert(10, Currency.USD, Currency.INR), 832.5);

        // The case that went negative — the adapter derives the inverse.
        assert close(converter.convert(832.5, Currency.INR, Currency.USD), 10.0);

        // Cross rate the vendor never quotes: INR -> EUR through USD.
        assert close(converter.convert(8325, Currency.INR, Currency.EUR), 92.0);

        assert converter.convert(5, Currency.EUR, Currency.EUR) == 5;

        // The -1 sentinel became an exception: a missing quote can't turn into a price.
        try {
            converter.convert(10, Currency.USD, Currency.GBP);
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }

        // The vendor SDK is untouched; all translation happened in the adapter.
        assert vendor.calls() > 0;

        // A test double needs no vendor at all — that's the seam the adapter created.
        ExchangeRates fixed = (from, to) -> 2.0;
        assert new PriceConverter(fixed).convert(3, Currency.USD, Currency.INR) == 6.0;

        System.out.println("solution: all asserts passed");
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 1e-9;
    }
}
