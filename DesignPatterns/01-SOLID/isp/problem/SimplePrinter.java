package isp.problem;

public class SimplePrinter implements Machine {
    private final String name;
    private int paper;

    public SimplePrinter(String name, int paper) {
        this.name = name;
        this.paper = paper;
    }

    @Override
    public void print(String document) {
        if (paper == 0) {
            System.out.println(name + " is out of paper");
            return;
        }
        paper--;
        System.out.println(name + " printed " + document + ", paper left: " + paper);
    }

    // forced on us by the fat interface — a simple printer has no scanner glass
    @Override
    public void scan(String document) {
        throw new UnsupportedOperationException(name + " cannot scan");
    }

    @Override
    public void copy(String document) {
        throw new UnsupportedOperationException(name + " cannot copy");
    }
}
