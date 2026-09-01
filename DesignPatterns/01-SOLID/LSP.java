// Liskov Substitution Principle — a subtype must work anywhere the base type works.
// Run: java LSP.java

public class LSP {
    public static void main(String[] args) {
        System.out.println("--- BAD: subtype that can't keep the base type's promise ---");
        BadFile file = new BadReadOnlyFile(); // the type says "File", so write() must work
        file.read();
        try {
            file.write(); // runtime explosion. The compiler could not warn us.
        } catch (UnsupportedOperationException e) {
            System.out.println("Blew up: " + e.getMessage());
        }

        System.out.println("\n--- GOOD: model capabilities, not a crippled subclass ---");
        ReadOnlyFile readOnly = new ReadOnlyFile();
        WritableFile writable = new WritableFile();

        readAnything(readOnly);   // both substitute cleanly for CanRead
        readAnything(writable);
        writable.write();
        // readAnything(...) can never be handed something that can't read,
        // and there is no way to call write() on a read-only file — it won't compile.
    }

    static void readAnything(CanRead source) { source.read(); }
}

// ---------- BAD ----------
class BadFile {
    void read()  { System.out.println("reading from file..."); }
    void write() { System.out.println("writing to file..."); }
}

class BadReadOnlyFile extends BadFile {
    @Override
    void write() { throw new UnsupportedOperationException("Can't write to a read-only file"); }
}

// ---------- GOOD ----------
// Split the contract by what a thing can actually do.
interface CanRead  { void read(); }
interface CanWrite { void write(); }

class ReadOnlyFile implements CanRead {
    @Override public void read() { System.out.println("Reading from a read-only file"); }
}

class WritableFile implements CanRead, CanWrite {
    @Override public void read()  { System.out.println("Reading from a writable file"); }
    @Override public void write() { System.out.println("Writing to a writable file"); }
}
