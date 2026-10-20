package observer.solution;

// Push an immutable event, not a bare double: adding a field later changes no listener signature.
// oldPrice is NaN for the first quote of a symbol.
public record PriceEvent(String symbol, double oldPrice, double newPrice) {
}
