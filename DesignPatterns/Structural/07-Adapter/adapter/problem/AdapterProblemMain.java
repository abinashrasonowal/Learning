package adapter.problem;

public class AdapterProblemMain {

    public static void main(String[] args) {
        PriceConverter converter = new PriceConverter();

        assert Math.abs(converter.convert(10, "USD", "INR") - 832.5) < 1e-9;

        // INR -> USD isn't quoted directly; the vendor returns -1 and we treat it as a rate.
        double price = converter.convert(832.5, "INR", "USD");
        assert price < 0;
        System.out.println("INR->USD came out negative: " + price);

        // Switching vendor means rewriting PriceConverter — it's welded to this SDK.
        System.out.println("problem: all asserts passed");
    }
}
