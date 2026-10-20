package templatemethod.problem;

import java.util.List;

public class TemplateProblemMain {

    public static void main(String[] args) {
        List<Sale> sales = List.of(new Sale("North", 120), new Sale("South", 80));

        assert new CsvReport().render(sales).equals("""
                region,amount
                North,120
                South,80
                """);

        assert new MarkdownReport().render(sales).equals("""
                | Region | Amount |
                |---|---|
                | North | 120 |
                | South | 80 |
                | **Total** | 200 |
                """);

        // Same bad input, different answers — the duplicated skeletons drifted apart.
        List<Sale> bad = List.of(new Sale("East", -50));
        try {
            new CsvReport().render(bad);
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("csv rejected: " + expected.getMessage());
        }
        String markdown = new MarkdownReport().render(bad);
        assert markdown.contains("-50");
        System.out.println("markdown accepted a negative amount");

        // A 3rd format (HTML) = a 3rd copy of the loop, the validation and the total.
        System.out.println("problem: all asserts passed");
    }
}
