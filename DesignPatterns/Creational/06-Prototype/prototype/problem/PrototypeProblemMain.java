package prototype.problem;

public class PrototypeProblemMain {

    public static void main(String[] args) {
        ServerConfig template = ServerConfig.webTemplate();

        // The client copies by hand: it must know every field, and it copies references.
        ServerConfig web2 = new ServerConfig("web-2", template.cpus(), template.ports(), template.disks());

        // Customise the copy...
        web2.ports().add(8443);
        web2.disks().get(1).resize(500);

        // ...and the template changed too — the lists and disks are shared.
        assert template.ports().contains(8443);
        assert template.disks().get(1).sizeGb() == 500;
        System.out.println("shallow copy: template now has port 8443 and a 500 GB data disk");

        // tags was added to ServerConfig later; this copy site never heard of it.
        assert template.tags().contains("tier:web");
        assert web2.tags().isEmpty();
        System.out.println("hand copy silently dropped the tags field");

        System.out.println("problem: all asserts passed");
    }
}
