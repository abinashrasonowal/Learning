package observer.problem;

public class PriceBoard {
    public void show(String symbol, double price) {
        System.out.println("[board] " + symbol + " = " + price);
    }
}
