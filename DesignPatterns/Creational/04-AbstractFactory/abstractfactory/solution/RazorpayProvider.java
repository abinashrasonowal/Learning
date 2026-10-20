package abstractfactory.solution;

public class RazorpayProvider implements PaymentProvider {

    @Override
    public Charger charger() {
        return new RazorpayCharger();
    }

    @Override
    public Refunder refunder() {
        return new RazorpayRefunder();
    }
}
