package lsp.problem;

public class LSPProblemMain {

    // Written against Rectangle. Knows nothing about which subtype it gets,
    // and assumes width and height move independently — which Rectangle promised.
    static void stretch(Rectangle rectangle) {
        rectangle.setWidth(5);
        rectangle.setHeight(4);
        System.out.println("expected area 20, got " + rectangle.area());
        assert rectangle.area() == 20 : "subtype broke the Rectangle contract";
    }

    public static void main(String[] args) {
        stretch(new Rectangle(1, 1));
        stretch(new Square(1));         // nothing throws — the answer is just wrong
    }
}
