package templatemethod.solution;

public class MarkdownExporter extends ReportExporter {

    @Override
    protected String header() {
        return "| Region | Amount |\n|---|---|\n";
    }

    @Override
    protected String row(Sale sale) {
        return "| " + sale.region() + " | " + sale.amount() + " |\n";
    }

    @Override
    protected String footer(int total) {
        return "| **Total** | " + total + " |\n";
    }
}
