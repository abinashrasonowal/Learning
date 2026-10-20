package observer.problem;

public class StockTicker {
    // One field per concrete listener. A 4th listener = a new field, a new constructor arg, a new call.
    private final PriceBoard board;
    private final AlertService alerts;
    private final TradeLogger logger;

    public StockTicker(PriceBoard board, AlertService alerts, TradeLogger logger) {
        this.board = board;
        this.alerts = alerts;
        this.logger = logger;
    }

    public void setPrice(String symbol, double price) {
        // Each listener has its own method name and signature — the ticker must know them all.
        board.show(symbol, price);
        alerts.check(symbol, price);
        logger.log(symbol, price);
    }
}
