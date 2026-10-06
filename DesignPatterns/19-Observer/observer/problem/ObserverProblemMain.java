package observer.problem;

public class ObserverProblemMain {

    public static void main(String[] args) {
        StockTicker ticker = new StockTicker(new PriceBoard(), new AlertService(150), new TradeLogger());

        ticker.setPrice("INFY", 140);
        ticker.setPrice("INFY", 155);
        ticker.setPrice("INFY", 160);   // alert fires again — nothing can unsubscribe it
        ticker.setPrice("INFY", 160);   // same price, everyone notified anyway

        // No way to add or remove a listener at runtime, no way to observe it in a test.
    }
}
