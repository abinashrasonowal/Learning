package abstractfactory.solution;

// The abstract factory: one method per product in the family. Whoever holds a provider
// can only ever get a charger and a refunder that belong together.
public interface PaymentProvider {
    Charger charger();

    Refunder refunder();
}
