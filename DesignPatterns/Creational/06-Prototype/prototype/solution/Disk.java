package prototype.solution;

public class Disk implements Prototype<Disk> {
    private final String name;
    private int sizeGb;

    public Disk(String name, int sizeGb) {
        this.name = name;
        this.sizeGb = sizeGb;
    }

    @Override
    public Disk copy() {
        return new Disk(name, sizeGb);
    }

    public void resize(int sizeGb) {
        this.sizeGb = sizeGb;
    }

    public String name() {
        return name;
    }

    public int sizeGb() {
        return sizeGb;
    }
}
