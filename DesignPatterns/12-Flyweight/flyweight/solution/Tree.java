package flyweight.solution;

// The context: extrinsic state (x, y) plus a reference to the shared flyweight.
// Two ints and a pointer per tree instead of a kilobyte.
public record Tree(int x, int y, TreeType type) {

    public String draw() {
        return type.draw(x, y);
    }
}
