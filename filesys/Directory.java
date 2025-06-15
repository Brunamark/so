package filesys;

import java.util.HashMap;
import java.util.Map;

public class Directory {
    private Metadata metadata;
    private Map<String, Directory> subDirectories = new HashMap<>();

    
    public Directory(Metadata metadata, Map<String, Directory> subDirectories) {
        this.metadata = metadata;
        this.subDirectories = subDirectories;
    }

    public Directory(String owner, String name) {
        this.metadata = new Metadata(name, owner);
        this.metadata.getPermissions().put(owner, "rwx"); 
        this.subDirectories = new HashMap<>();
    }
    
    public Metadata getMetadata() {
        return metadata;
    }
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
    public Map<String, Directory> getSubDirectories() {
        return subDirectories;
    }
    public void setSubDirectories(Map<String, Directory> subDirectories) {
        this.subDirectories = subDirectories;
    }
    
}
