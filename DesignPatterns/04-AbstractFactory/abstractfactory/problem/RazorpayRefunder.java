package abstractfactory.problem;

public class RazorpayRefunder {

    public String refund(String paymentId) {
        if (!paymentId.startsWith("pay_")) {
            throw new IllegalArgumentException("Razorpay can't refund a non-Razorpay payment: " + paymentId);
        }
        return "rfnd_" + paymentId.substring(4);
    }
}
