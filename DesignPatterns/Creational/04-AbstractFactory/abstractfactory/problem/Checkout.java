package abstractfactory.problem;

// Picks each concrete class by hand. Someone moved refunds to Razorpay and forgot that
// charges still go through Stripe — the two halves no longer belong to the same family.
public class Checkout {
    private final StripeCharger charger = new StripeCharger();
    private final RazorpayRefunder refunder = new RazorpayRefunder();

    public String pay(int amount) {
        return charger.charge(amount);
    }

    public String refund(String chargeId) {
        return refunder.refund(chargeId);
    }
}
