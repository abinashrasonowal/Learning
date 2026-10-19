package iterator.solution;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorSolutionMain {

    public static void main(String[] args) {
        Playlist playlist = new Playlist();
        playlist.add(new Track("Intro", 60, false));
        playlist.add(new Track("Loud", 200, true));
        playlist.add(new Track("Calm", 180, false));

        // Same numbers as problem/ — but for-each works because Playlist is Iterable.
        int total = 0;
        for (Track track : playlist) {
            total += track.seconds();
        }
        assert total == 440;

        // The filter lives in the collection, written once.
        int clean = 0;
        for (Track track : playlist.cleanOnly()) {
            assert !track.explicit();
            clean++;
        }
        assert clean == 2;

        // Two iterators at once, each with its own cursor.
        Iterator<Track> a = playlist.iterator();
        Iterator<Track> b = playlist.iterator();
        assert a.next().title().equals("Intro");
        assert a.next().title().equals("Loud");
        assert b.next().title().equals("Intro");

        // Exhaustion is a contract: NoSuchElementException, not an index or null error.
        Iterator<Track> it = playlist.iterator();
        it.next();
        it.next();
        it.next();
        assert !it.hasNext();
        try {
            it.next();
            assert false : "expected NoSuchElementException";
        } catch (NoSuchElementException expected) {
            System.out.println("exhausted: " + expected.getMessage());
        }

        // remove() wasn't overridden — the default refuses rather than corrupting.
        try {
            playlist.iterator().remove();
            assert false : "expected UnsupportedOperationException";
        } catch (UnsupportedOperationException expected) {
            System.out.println("remove: unsupported");
        }

        // Fail fast: changing the playlist mid-loop is caught on the next step.
        try {
            for (Track track : playlist) {
                playlist.add(new Track("Encore of " + track.title(), 30, false));
            }
            assert false : "expected ConcurrentModificationException";
        } catch (ConcurrentModificationException expected) {
            System.out.println("fail-fast: " + expected.getMessage());
        }

        System.out.println("solution: all asserts passed");
    }
}
