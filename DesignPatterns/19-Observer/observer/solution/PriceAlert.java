package observer.solution;

// Fires once when the price crosses the threshold upwards, then unsubscribes itself.
public class PriceAlert implements PriceListener {
    private final StockTicker ticker;
    private final String symbol;
    private final double threshold;
    private int fired;

    public PriceAlert(StockTicker ticker, String symbol, double threshold) {
        this.ticker = ticker;
        this.symbol = symbol;
        this.threshold = threshold;
    }

    @Override
    public void onPrice(PriceEvent event) {
        if (!event.symbol().equals(symbol)) {
            return;
        }
        if (event.oldPrice() < threshold && event.newPrice() >= threshold) {
            fired++;
            System.out.println("[alert] " + symbol + " crossed " + threshold);
            ticker.detach(this);   // detach during notify — safe only because the ticker iterates a copy
        }
    }

    public int timesFired() {
        return fired;
    }
}
