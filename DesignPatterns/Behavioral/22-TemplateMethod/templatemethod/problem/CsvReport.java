package templatemethod.problem;

import java.util.List;

public class CsvReport {

    public String render(List<Sale> sales) {
        if (sales.isEmpty()) {
            throw new IllegalArgumentException("no sales to report");
        }
        for (Sale sale : sales) {
            if (sale.amount() < 0) {
                throw new IllegalArgumentException("negative amount for " + sale.region());
            }
        }
        StringBuilder out = new StringBuilder("region,amount\n");
        for (Sale sale : sales) {
            out.append(sale.region()).append(',').append(sale.amount()).append('\n');
        }
        return out.toString();
    }
}
