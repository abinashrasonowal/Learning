package templatemethod.problem;

import java.util.List;

// Copy of CsvReport's skeleton with different formatting. The copies have already drifted:
// the negative-amount check never made it over here.
public class MarkdownReport {

    public String render(List<Sale> sales) {
        if (sales.isEmpty()) {
            throw new IllegalArgumentException("no sales to report");
        }
        StringBuilder out = new StringBuilder("| Region | Amount |\n|---|---|\n");
        int total = 0;
        for (Sale sale : sales) {
            out.append("| ").append(sale.region()).append(" | ").append(sale.amount()).append(" |\n");
            total += sale.amount();
        }
        out.append("| **Total** | ").append(total).append(" |\n");
        return out.toString();
    }
}
