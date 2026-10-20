package composite.solution;

// The component: what a leaf and a group have in common. Clients only use this.
public interface Item {
    int price();

    int productCount();

    void describe(StringBuilder out, String indent);
}
