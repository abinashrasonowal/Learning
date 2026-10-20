package adapter.solution;

// The target: the interface OUR code wants, in our types, with exceptions for errors.
public interface ExchangeRates {
    double rate(Currency from, Currency to);
}
