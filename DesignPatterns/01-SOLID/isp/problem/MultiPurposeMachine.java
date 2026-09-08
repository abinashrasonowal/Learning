package isp.problem;

public class MultiPurposeMachine implements Machine {
    private final String name;
    private int paper;

    public MultiPurposeMachine(String name, int paper) {
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

    @Override
    public void scan(String document) {
        System.out.println(name + " scanned " + document);
    }

    @Override
    public void copy(String document) {
        scan(document);
        print(document);
    }
}
