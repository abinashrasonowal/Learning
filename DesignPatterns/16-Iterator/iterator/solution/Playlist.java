package iterator.solution;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

// Storage is a hand-rolled linked list now — and no caller can tell, because none of
// them ever sees it. They only see Iterator<Track>.
public class Playlist implements Iterable<Track> {

    private static final class Node {
        final Track track;
        Node next;

        Node(Track track) {
            this.track = track;
        }
    }

    private Node head;
    private Node tail;
    private int modCount;   // bumped on every structural change, so live iterators can fail fast

    public void add(Track track) {
        Node node = new Node(track);
        if (head == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        modCount++;
    }

    @Override
    public Iterator<Track> iterator() {
        return new TrackIterator(track -> true);
    }

    // A second traversal is a new iterator, not a new loop in every caller.
    public Iterable<Track> cleanOnly() {
        return () -> new TrackIterator(track -> !track.explicit());
    }

    // Inner (non-static) so it can read head and modCount. Each call gets its own cursor.
    private final class TrackIterator implements Iterator<Track> {
        private final Predicate<Track> filter;
        private final int expectedModCount = modCount;
        private Node cursor;

        TrackIterator(Predicate<Track> filter) {
            this.filter = filter;
            this.cursor = skipToMatch(head);
        }

        @Override
        public boolean hasNext() {
            return cursor != null;
        }

        @Override
        public Track next() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException("playlist changed during iteration");
            }
            if (cursor == null) {
                throw new NoSuchElementException("no more tracks");
            }
            Track track = cursor.track;
            cursor = skipToMatch(cursor.next);
            return track;
        }

        private Node skipToMatch(Node from) {
            Node node = from;
            while (node != null && !filter.test(node.track)) {
                node = node.next;
            }
            return node;
        }
    }
}
