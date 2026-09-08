package isp.solution;

// implements exactly the one role it has — no stubs to write, nothing to throw
public class SimplePrinter implements Printer {
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
}
