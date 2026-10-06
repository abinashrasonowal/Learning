package command.problem;

public class CommandProblemMain {

    public static void main(String[] args) {
        Document doc = new Document();

        new AppendButton(doc, "hello").click();
        new AppendButton(doc, " world").click();
        assert doc.text().equals("hello world");

        new DeleteButton(doc, 6).click();
        assert doc.text().equals("hello");

        new UpperButton(doc).click();
        assert doc.text().equals("HELLO");

        // Undo? Each button fires and forgets. There is no record of what happened,
        // no object holding the deleted " world" or the pre-uppercase "hello".
        // A shortcut key doing "bold" would need yet another class calling doc directly.
        System.out.println("problem: " + doc.text() + " — and no way back");
    }
}
