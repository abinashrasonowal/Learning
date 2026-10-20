package templatemethod.solution;

public class CsvExporter extends ReportExporter {

    @Override
    protected String header() {
        return "region,amount\n";
    }

    @Override
    protected String row(Sale sale) {
        return sale.region() + "," + sale.amount() + "\n";
    }
}
