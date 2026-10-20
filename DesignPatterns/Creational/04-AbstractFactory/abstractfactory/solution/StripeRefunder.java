package abstractfactory.solution;

class StripeRefunder implements Refunder {

    @Override
    public String refund(String chargeId) {
        if (!chargeId.startsWith("ch_")) {
            throw new IllegalArgumentException("Stripe can't refund a non-Stripe charge: " + chargeId);
        }
        return "re_" + chargeId.substring(3);
    }
}
