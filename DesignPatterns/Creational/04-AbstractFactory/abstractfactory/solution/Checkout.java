package abstractfactory.solution;

// Takes one provider, pulls both halves from it. There is no constructor that accepts
// a charger and a refunder separately, so a mixed pair can't be built here.
public class Checkout {
    private final Charger charger;
    private final Refunder refunder;

    public Checkout(PaymentProvider provider) {
        this.charger = provider.charger();
        this.refunder = provider.refunder();
    }

    public String pay(int amount) {
        return charger.charge(amount);
    }

    public String refund(String chargeId) {
        return refunder.refund(chargeId);
    }
}
