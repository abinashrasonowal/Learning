package abstractfactory.problem;

public class AbstractFactoryProblemMain {

    public static void main(String[] args) {
        Checkout checkout = new Checkout();

        String chargeId = checkout.pay(500);
        assert chargeId.equals("ch_1");

        // Compiles fine, fails at runtime — a Stripe charge sent to the Razorpay refunder.
        try {
            checkout.refund(chargeId);
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("mismatched family: " + expected.getMessage());
        }

        // Switching Checkout to Razorpay = editing two field types and two `new`s, and
        // nothing stops you from changing only one of them again.
        System.out.println("problem: all asserts passed");
    }
}
