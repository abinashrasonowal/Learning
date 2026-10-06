package observer.solution;

import java.util.ConcurrentModificationException;

public class ObserverSolutionMain {

    @SuppressWarnings("try")   // the Subscription is held only for its close() — that's the point
    public static void main(String[] args) {
        naiveTickerBreaks();

        StockTicker ticker = new StockTicker();
        PriceBoard board = new PriceBoard();
        PriceAlert alert = new PriceAlert(ticker, "INFY", 150);
        TradeLogger logger = new TradeLogger();

        ticker.attach(alert);     // first, so its self-detach happens while others are still pending
        ticker.attach(board);
        ticker.attach(logger);
        ticker.attach(logger);    // duplicate — must not double-notify
        ticker.attach(event -> { throw new IllegalStateException("broken listener"); });
        ticker.attach(event -> { }); // a listener after the broken one, to prove the loop continues

        ticker.setPrice("INFY", 140);
        ticker.setPrice("INFY", 155);   // alert fires and detaches itself mid-notify
        ticker.setPrice("INFY", 160);   // alert is gone — fires no more
        ticker.setPrice("INFY", 160);   // unchanged price — nobody notified

        assert logger.events().size() == 3 : "logger saw " + logger.events().size();
        assert logger.events().get(1).equals(new PriceEvent("INFY", 140, 155));
        assert Double.isNaN(logger.events().get(0).oldPrice());
        assert alert.timesFired() == 1;
        assert board.price("INFY") == 160;
        assert ticker.listenerCount() == 4 : "alert should have detached, got " + ticker.listenerCount();

        // Subscription: listener lives exactly as long as the try block — no lapsed listener.
        TradeLogger shortLived = new TradeLogger();
        try (Subscription ignored = ticker.attach(shortLived)) {
            ticker.setPrice("TCS", 3900);
        }
        ticker.setPrice("TCS", 3950);
        assert shortLived.events().size() == 1 : "got events after close: " + shortLived.events();

        System.out.println("solution: all asserts passed");
    }

    private static void naiveTickerBreaks() {
        NaiveStockTicker naive = new NaiveStockTicker();
        naive.attach(new PriceListener() {
            @Override
            public void onPrice(PriceEvent event) {
                naive.detach(this);
            }
        });
        naive.attach(event -> { });
        naive.attach(event -> { });

        try {
            naive.setPrice("INFY", 100);
            assert false : "expected ConcurrentModificationException";
        } catch (ConcurrentModificationException expected) {
            System.out.println("naive ticker: ConcurrentModificationException on self-detach");
        }
    }
}
