package observer.problem;

public class AlertService {
    private final double threshold;

    public AlertService(double threshold) {
        this.threshold = threshold;
    }

    public void check(String symbol, double price) {
        if (price >= threshold) {
            System.out.println("[alert] " + symbol + " crossed " + threshold);
        }
    }
}
