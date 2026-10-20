package flyweight.solution;

import java.util.ArrayList;
import java.util.List;

public class Forest {
    private final TreeTypeFactory types;
    private final List<Tree> trees = new ArrayList<>();

    public Forest(TreeTypeFactory types) {
        this.types = types;
    }

    public void plant(int x, int y, String name, String color) {
        trees.add(new Tree(x, y, types.get(name, color)));
    }

    public Tree tree(int index) {
        return trees.get(index);
    }

    public int size() {
        return trees.size();
    }
}
