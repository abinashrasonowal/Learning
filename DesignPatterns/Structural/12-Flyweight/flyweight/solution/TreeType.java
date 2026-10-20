package flyweight.solution;

// The flyweight: intrinsic state only (what every oak shares). Immutable — it's shared by
// hundreds of trees, so a change here would change all of them.
public final class TreeType {
    private final String name;
    private final String color;
    private final byte[] texture;

    TreeType(String name, String color, byte[] texture) {
        this.name = name;
        this.color = color;
        this.texture = texture;     // never handed out, never written
    }

    // Extrinsic state (the position) is passed in, not stored.
    public String draw(int x, int y) {
        return name + "/" + color + "@" + x + "," + y;
    }

    public int textureBytes() {
        return texture.length;
    }
}
