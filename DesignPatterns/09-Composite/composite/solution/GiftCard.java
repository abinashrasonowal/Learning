package composite.solution;

// A new leaf. It answers price() itself — nothing else had to change.
public record GiftCard(int value) implements Item {

    @Override
    public int price() {
        return value;
    }

    @Override
    public int productCount() {
        return 0;
    }

    @Override
    public void describe(StringBuilder out, String indent) {
        out.append(indent).append("Gift card ").append(value).append('\n');
    }
}
