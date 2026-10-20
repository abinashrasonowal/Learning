package facade.solution;

public record OrderRequest(String email, String sku, int qty, int amount, String card, String address) {
}
