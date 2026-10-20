package observer.problem;

public class TradeLogger {
    public void log(String symbol, double price) {
        System.out.println("[log] " + symbol + " -> " + price);
    }
}
