package flyweight.solution;

public class FlyweightSolutionMain {

    public static void main(String[] args) {
        TextureLoader loader = new TextureLoader();
        TreeTypeFactory types = new TreeTypeFactory(loader);
        Forest forest = new Forest(types);
        String[][] kinds = {{"oak", "green"}, {"pine", "dark-green"}, {"birch", "white"}};

        // Same 1000 trees as problem/.
        for (int i = 0; i < 1000; i++) {
            String[] kind = kinds[i % kinds.length];
            forest.plant(i, i * 2, kind[0], kind[1]);
        }

        // 3 kinds -> 3 loads, 3 KB of textures, instead of 1000 and ~1 MB.
        assert forest.size() == 1000;
        assert types.distinctTypes() == 3;
        assert loader.loads() == 3;
        assert types.textureBytes() == 3L * TextureLoader.TEXTURE_BYTES;
        System.out.println("solution: " + loader.loads() + " texture loads for " + forest.size() + " trees");

        // Trees of the same kind share one TreeType object — identity, not equality.
        assert forest.tree(0).type() == forest.tree(3).type();
        assert forest.tree(0).type() != forest.tree(1).type();
        assert types.get("oak", "green") == forest.tree(0).type();

        // Same name, different color -> a different flyweight; the key is all intrinsic state.
        assert types.get("oak", "autumn-red") != types.get("oak", "green");
        assert types.distinctTypes() == 4;

        // Extrinsic state still differs per tree.
        assert forest.tree(0).draw().equals("oak/green@0,0");
        assert forest.tree(3).draw().equals("oak/green@3,6");

        System.out.println("solution: all asserts passed");
    }
}
