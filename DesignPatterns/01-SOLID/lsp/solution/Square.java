package lsp.solution;

// not a Rectangle — it cannot keep Rectangle's promise that the two sides move apart
public class Square implements Shape {
    private final double side;

    public Square(double side) {
        this.side = side;
    }

    public Square resized(double side) {
        return new Square(side);
    }

    @Override
    public double area() {
        return side * side;
    }

    @Override
    public String toString() {
        return "Square " + side;
    }
}
