package lsp.solution;

public class Rectangle implements Shape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    // no setters — resizing hands back a new value instead of mutating a shared one
    public Rectangle resized(double width, double height) {
        return new Rectangle(width, height);
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public String toString() {
        return "Rectangle " + width + "x" + height;
    }
}
