package flyweight.problem;

// Stands in for reading an image from disk. Counts loads so the cost is visible.
public class TextureLoader {
    public static final int TEXTURE_BYTES = 1024;
    private int loads;

    public byte[] load(String name) {
        loads++;
        byte[] texture = new byte[TEXTURE_BYTES];
        texture[0] = (byte) name.hashCode();
        return texture;
    }

    public int loads() {
        return loads;
    }
}
