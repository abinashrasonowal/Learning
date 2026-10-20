package prototype.problem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerConfig {
    private String name;
    private final int cpus;
    private final List<Integer> ports;
    private final List<Disk> disks;
    private final Set<String> tags = new HashSet<>();   // added later — not in the constructor

    public ServerConfig(String name, int cpus, List<Integer> ports, List<Disk> disks) {
        this.name = name;
        this.cpus = cpus;
        this.ports = ports;
        this.disks = disks;
    }

    public static ServerConfig webTemplate() {
        List<Disk> disks = new ArrayList<>();
        disks.add(new Disk("root", 50));
        disks.add(new Disk("data", 200));
        ServerConfig web = new ServerConfig("web", 4, new ArrayList<>(List.of(80, 443)), disks);
        web.addTag("tier:web");
        return web;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public String name() {
        return name;
    }

    public int cpus() {
        return cpus;
    }

    public List<Integer> ports() {
        return ports;
    }

    public List<Disk> disks() {
        return disks;
    }

    public Set<String> tags() {
        return tags;
    }
}
