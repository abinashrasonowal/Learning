package abstractfactory.solution;

// Concrete factory. The product classes are package-private, so outside this package
// the only way to get a Stripe charger is through here — paired with a Stripe refunder.
public class StripeProvider implements PaymentProvider {

    @Override
    public Charger charger() {
        return new StripeCharger();
    }

    @Override
    public Refunder refunder() {
        return new StripeRefunder();
    }
}
