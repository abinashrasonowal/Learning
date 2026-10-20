package abstractfactory.solution;

public class AbstractFactorySolutionMain {

    public static void main(String[] args) {
        // The flow that failed in problem/ — now the pair always matches.
        Checkout stripe = new Checkout(new StripeProvider());
        String chargeId = stripe.pay(500);
        assert chargeId.equals("ch_1");
        assert stripe.refund(chargeId).equals("re_1");

        // Switching provider is one argument, and both halves switch together.
        Checkout razorpay = new Checkout(new RazorpayProvider());
        String paymentId = razorpay.pay(500);
        assert paymentId.equals("pay_1");
        assert razorpay.refund(paymentId).equals("rfnd_1");

        // Ids from another family are still rejected — the products keep their own rules.
        try {
            razorpay.refund(chargeId);
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }

        // A 3rd family with no new files: Charger and Refunder are one-method interfaces.
        PaymentProvider fake = new PaymentProvider() {
            private int next = 1;

            @Override
            public Charger charger() {
                return amount -> "fake_" + next++;
            }

            @Override
            public Refunder refunder() {
                return id -> "fake_refund_" + id;
            }
        };
        Checkout test = new Checkout(fake);
        assert test.refund(test.pay(100)).equals("fake_refund_fake_1");

        // Provider chosen at the edge, from config — Checkout never names a provider.
        String configured = args.length > 0 ? args[0] : "razorpay";
        PaymentProvider provider = configured.equals("stripe") ? new StripeProvider() : new RazorpayProvider();
        System.out.println(configured + " -> " + new Checkout(provider).pay(250));

        System.out.println("solution: all asserts passed");
    }
}
