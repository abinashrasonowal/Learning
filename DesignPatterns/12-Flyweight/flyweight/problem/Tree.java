package flyweight.problem;

// Every tree carries its own copy of data that's identical for every tree of its kind.
public class Tree {
    private final int x;
    private final int y;
    private final String name;
    private final String color;
    private final byte[] texture;

    public Tree(int x, int y, String name, String color, byte[] texture) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.color = color;
        this.texture = texture;
    }

    public String draw() {
        return name + "/" + color + "@" + x + "," + y;
    }

    public int textureBytes() {
        return texture.length;
    }
}
