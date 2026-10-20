package facade.problem;

import java.util.ArrayList;
import java.util.List;

public class PaymentService {
    private final List<String> refunded = new ArrayList<>();
    private int next = 1;

    public String charge(String card, int amount) {
        if (card.equals("declined")) {
            throw new IllegalStateException("card declined");
        }
        return "txn_" + next++;
    }

    public void refund(String txnId) {
        refunded.add(txnId);
    }

    public List<String> refunded() {
        return refunded;
    }
}
