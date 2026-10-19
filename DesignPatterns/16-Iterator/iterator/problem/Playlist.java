package iterator.problem;

import java.util.Arrays;

public class Playlist {
    private Track[] tracks = new Track[2];
    private int count;

    public void add(Track track) {
        if (count == tracks.length) {
            tracks = Arrays.copyOf(tracks, tracks.length * 2);
        }
        tracks[count++] = track;
    }

    // Leaks the internal array: callers now depend on "it's an array", on count vs length,
    // and can write into it. Switch storage to a linked list and every caller breaks.
    public Track[] getTracks() {
        return tracks;
    }

    public int size() {
        return count;
    }
}
