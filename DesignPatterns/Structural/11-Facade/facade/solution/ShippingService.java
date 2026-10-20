package facade.solution;

public class ShippingService {
    private int next = 1;

    public String ship(String sku, int qty, String address) {
        if (address.isBlank()) {
            throw new IllegalArgumentException("no shipping address");
        }
        return "shp_" + next++;
    }
}
