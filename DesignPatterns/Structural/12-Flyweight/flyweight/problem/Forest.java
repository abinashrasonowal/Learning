package flyweight.problem;

import java.util.ArrayList;
import java.util.List;

public class Forest {
    private final TextureLoader loader;
    private final List<Tree> trees = new ArrayList<>();

    public Forest(TextureLoader loader) {
        this.loader = loader;
    }

    public void plant(int x, int y, String name, String color) {
        trees.add(new Tree(x, y, name, color, loader.load(name)));   // a fresh texture per tree
    }

    public long textureBytes() {
        long total = 0;
        for (Tree tree : trees) {
            total += tree.textureBytes();
        }
        return total;
    }

    public int size() {
        return trees.size();
    }
}
