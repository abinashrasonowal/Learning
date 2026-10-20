package iterator.problem;

public class IteratorProblemMain {

    public static void main(String[] args) {
        Playlist playlist = new Playlist();
        playlist.add(new Track("Intro", 60, false));
        playlist.add(new Track("Loud", 200, true));
        playlist.add(new Track("Calm", 180, false));

        // Every caller writes the index loop and must remember size(), not length.
        int total = 0;
        for (int i = 0; i < playlist.size(); i++) {
            total += playlist.getTracks()[i].seconds();
        }
        assert total == 440;

        // Capacity grew to 4, so length lies — the 4th slot is null.
        assert playlist.getTracks().length == 4;
        assert playlist.getTracks()[3] == null;

        // "Clean tracks only" — the filter is copy-pasted into every caller that needs it.
        int clean = 0;
        for (int i = 0; i < playlist.size(); i++) {
            if (!playlist.getTracks()[i].explicit()) {
                clean++;
            }
        }
        assert clean == 2;

        // Any caller can corrupt the collection through the leaked array.
        playlist.getTracks()[0] = null;
        try {
            playlist.getTracks()[0].seconds();
            assert false : "expected NPE";
        } catch (NullPointerException expected) {
            System.out.println("corrupted from outside: slot 0 is null");
        }

        System.out.println("problem: all asserts passed");
    }
}
