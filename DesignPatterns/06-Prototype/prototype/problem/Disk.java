package prototype.problem;

public class Disk {
    private final String name;
    private int sizeGb;

    public Disk(String name, int sizeGb) {
        this.name = name;
        this.sizeGb = sizeGb;
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
