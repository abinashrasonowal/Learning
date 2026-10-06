package observer.solution;

import java.util.ArrayList;
import java.util.List;

// Records every event — doubles as the test double the asserts read from.
public class TradeLogger implements PriceListener {
    private final List<PriceEvent> events = new ArrayList<>();

    @Override
    public void onPrice(PriceEvent event) {
        events.add(event);
    }

    public List<PriceEvent> events() {
        return List.copyOf(events);
    }
}
