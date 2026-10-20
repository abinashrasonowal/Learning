package adapter.solution;

// Knows nothing about the vendor. Any ExchangeRates works — the adapter, a fixed table in
// tests, or a second vendor's adapter later.
public class PriceConverter {
    private final ExchangeRates rates;

    public PriceConverter(ExchangeRates rates) {
        this.rates = rates;
    }

    public double convert(double amount, Currency from, Currency to) {
        return amount * rates.rate(from, to);
    }
}
