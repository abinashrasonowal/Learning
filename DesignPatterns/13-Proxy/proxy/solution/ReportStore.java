package proxy.solution;

public interface ReportStore {
    String read(String id);

    void delete(String id);

    boolean exists(String id);
}
