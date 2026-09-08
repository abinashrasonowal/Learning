package lsp.problem;

public class Square extends Rectangle {

    public Square(double side) {
        super(side, side);
    }

    // a square must keep its sides equal — so each setter silently moves the other
    @Override
    public void setWidth(double width) {
        this.width = width;
        this.height = width;
    }

    @Override
    public void setHeight(double height) {
        this.width = height;
        this.height = height;
    }
}
