package composite.solution;

// Leaf.
public record Product(String name, int price) implements Item {

    @Override
    public int productCount() {
        return 1;
    }

    @Override
    public void describe(StringBuilder out, String indent) {
        out.append(indent).append(name).append(' ').append(price).append('\n');
    }
}
