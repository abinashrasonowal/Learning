package proxy.problem;

public record User(String name, String role) {

    public boolean isAdmin() {
        return role.equals("admin");
    }
}
