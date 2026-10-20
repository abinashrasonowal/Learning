package factory.solution;

public interface Storage {
    void put(String key, String value);

    String get(String key);

    String describe();
}
