package lsp.solution;

public class LSPSolutionMain {

    static void stretch(Rectangle rectangle) {
        Rectangle stretched = rectangle.resized(5, 4);
        System.out.println(stretched + " area " + stretched.area());
        assert stretched.area() == 20 : "Rectangle broke its own contract";
    }

    // both types ARE substitutable here — area() is a promise each one can keep
    static double totalArea(Shape... shapes) {
        double total = 0;
        for (Shape shape : shapes) {
            total += shape.area();
        }
        return total;
    }

    public static void main(String[] args) {
        stretch(new Rectangle(1, 1));

        Square square = new Square(3);
        System.out.println(square + " area " + square.area());
        // stretch(square);         // Required type:
                                    // Rectangle
                                    // Provided:
                                    // Square

        System.out.println("total " + totalArea(new Rectangle(2, 3), square));
    }
}
