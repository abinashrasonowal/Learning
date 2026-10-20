package prototype.solution;

public class PrototypeSolutionMain {

    public static void main(String[] args) {
        ServerConfig template = new ServerConfig("web", 4)
                .addPort(80).addPort(443)
                .addDisk("root", 50).addDisk("data", 200)
                .addTag("tier:web");

        // Same customisation as problem/ — the template is untouched this time.
        ServerConfig web2 = template.copy().rename("web-2").addPort(8443);
        web2.disk("data").resize(500);

        assert !template.ports().contains(8443);
        assert template.disk("data").sizeGb() == 200;
        assert web2.ports().contains(8443) && web2.disk("data").sizeGb() == 500;

        // Every field came across, including the one added later — copy() knows them all.
        assert web2.tags().contains("tier:web");
        assert web2.cpus() == 4;

        // Callers can't reach the internals to share them by accident.
        try {
            web2.ports().add(22);
            assert false : "expected UnsupportedOperationException";
        } catch (UnsupportedOperationException expected) {
            System.out.println("ports are read-only from outside");
        }

        // Registry: named templates, a fresh independent copy per request.
        PrototypeRegistry<ServerConfig> registry = new PrototypeRegistry<>();
        registry.register("web", template);
        registry.register("db", new ServerConfig("db", 8).addPort(5432).addDisk("data", 1000).addTag("tier:db"));

        ServerConfig a = registry.create("web");
        ServerConfig b = registry.create("web");
        assert a != b;
        a.disk("data").resize(999);
        assert b.disk("data").sizeGb() == 200;

        // Editing the object you registered doesn't change what the registry hands out.
        template.addPort(9000);
        assert !registry.create("web").ports().contains(9000);

        assert registry.create("db").disk("data").sizeGb() == 1000;

        try {
            registry.create("cache");
            assert false : "expected IllegalArgumentException";
        } catch (IllegalArgumentException expected) {
            System.out.println("rejected: " + expected.getMessage());
        }

        System.out.println("solution: all asserts passed");
    }
}
