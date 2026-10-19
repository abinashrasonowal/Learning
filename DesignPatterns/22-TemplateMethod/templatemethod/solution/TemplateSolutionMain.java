package templatemethod.solution;

import java.util.List;

public class TemplateSolutionMain {

    public static void main(String[] args) {
        List<Sale> sales = List.of(new Sale("North", 120), new Sale("South", 80));

        // Same output as problem/ — behaviour preserved.
        assert new CsvExporter().export(sales).equals("""
                region,amount
                North,120
                South,80
                """);

        assert new MarkdownExporter().export(sales).equals("""
                | Region | Amount |
                |---|---|
                | North | 120 |
                | South | 80 |
                | **Total** | 200 |
                """);

        // The drift is gone: one validate(), so every format rejects the same input.
        List<Sale> bad = List.of(new Sale("East", -50));
        expectRejected(() -> new CsvExporter().export(bad));
        expectRejected(() -> new MarkdownExporter().export(bad));
        expectRejected(() -> new MarkdownExporter().export(List.of()));

        // A 3rd format writes only its steps — no loop, no validation, no total.
        ReportExporter html = new ReportExporter() {
            @Override
            protected String header() {
                return "<table>\n";
            }

            @Override
            protected String row(Sale sale) {
                return "<tr><td>" + sale.region() + "</td><td>" + sale.amount() + "</td></tr>\n";
            }

            @Override
            protected String footer(int total) {
                return "<tr><th>Total</th><th>" + total + "</th></tr>\n</table>\n";
            }
        };
        assert html.export(sales).endsWith("<th>200</th></tr>\n</table>\n");
        expectRejected(() -> html.export(bad));

        System.out.println("solution: all asserts passed");
    }

    private static void expectRejected(Runnable action) {
        try {
            action.run();
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }
    }
}
