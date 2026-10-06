package observer.solution;

@FunctionalInterface
public interface PriceListener {
    void onPrice(PriceEvent event);
}
