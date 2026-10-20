package prototype.solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class ServerConfig implements Prototype<ServerConfig> {
    private String name;
    private final int cpus;
    private final List<Integer> ports = new ArrayList<>();
    private final List<Disk> disks = new ArrayList<>();
    private final Set<String> tags = new HashSet<>();

    public ServerConfig(String name, int cpus) {
        this.name = name;
        this.cpus = cpus;
    }

    // Copy constructor: the one place that knows every field. Add a field -> add it here,
    // and every copy anywhere picks it up.
    private ServerConfig(ServerConfig other) {
        this.name = other.name;
        this.cpus = other.cpus;
        this.ports.addAll(other.ports);              // Integers are immutable — sharing is fine
        for (Disk disk : other.disks) {
            this.disks.add(disk.copy());             // Disks are mutable — deep copy
        }
        this.tags.addAll(other.tags);
    }

    @Override
    public ServerConfig copy() {
        return new ServerConfig(this);
    }

    public ServerConfig rename(String name) {
        this.name = name;
        return this;
    }

    public ServerConfig addPort(int port) {
        ports.add(port);
        return this;
    }

    public ServerConfig addDisk(String diskName, int sizeGb) {
        disks.add(new Disk(diskName, sizeGb));
        return this;
    }

    public ServerConfig addTag(String tag) {
        tags.add(tag);
        return this;
    }

    public Disk disk(String diskName) {
        for (Disk disk : disks) {
            if (disk.name().equals(diskName)) {
                return disk;
            }
        }
        throw new NoSuchElementException("no disk " + diskName + " on " + name);
    }

    public String name() {
        return name;
    }

    public int cpus() {
        return cpus;
    }

    public List<Integer> ports() {
        return Collections.unmodifiableList(ports);
    }

    public Set<String> tags() {
        return Collections.unmodifiableSet(tags);
    }
}
