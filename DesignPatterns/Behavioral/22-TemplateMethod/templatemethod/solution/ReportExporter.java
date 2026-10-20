package templatemethod.solution;

import java.util.List;

public abstract class ReportExporter {

    // The template method. final: a subclass can fill in steps, never reorder or skip them.
    public final String export(List<Sale> sales) {
        validate(sales);
        StringBuilder out = new StringBuilder(header());
        int total = 0;
        for (Sale sale : sales) {
            out.append(row(sale));
            total += sale.amount();
        }
        out.append(footer(total));
        return out.toString();
    }

    // Fixed step — private, so no format can "forget" it the way MarkdownReport did.
    private void validate(List<Sale> sales) {
        if (sales.isEmpty()) {
            throw new IllegalArgumentException("no sales to report");
        }
        for (Sale sale : sales) {
            if (sale.amount() < 0) {
                throw new IllegalArgumentException("negative amount for " + sale.region());
            }
        }
    }

    // Required steps — every format must say how.
    protected abstract String header();

    protected abstract String row(Sale sale);

    // Hook — optional, empty by default.
    protected String footer(int total) {
        return "";
    }
}
