package prototype.solution;

// Named copy(), not clone(): Object.clone() and Cloneable come with a shallow default
// and a checked exception. The object copies itself — callers never list its fields.
public interface Prototype<T> {
    T copy();
}
