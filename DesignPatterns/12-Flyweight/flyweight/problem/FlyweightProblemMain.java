package flyweight.problem;

public class FlyweightProblemMain {

    public static void main(String[] args) {
        TextureLoader loader = new TextureLoader();
        Forest forest = new Forest(loader);
        String[][] kinds = {{"oak", "green"}, {"pine", "dark-green"}, {"birch", "white"}};

        for (int i = 0; i < 1000; i++) {
            String[] kind = kinds[i % kinds.length];
            forest.plant(i, i * 2, kind[0], kind[1]);
        }

        // 3 kinds of tree, 1000 textures loaded and kept in memory.
        assert forest.size() == 1000;
        assert loader.loads() == 1000;
        assert forest.textureBytes() == 1000L * TextureLoader.TEXTURE_BYTES;
        System.out.println("problem: " + loader.loads() + " texture loads, "
                + forest.textureBytes() / 1024 + " KB of textures for 3 kinds of tree");

        System.out.println("problem: all asserts passed");
    }
}
