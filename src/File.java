import java.nio.file.Path;
import java.nio.file.Paths;

public class File {
    public String name;
    public Path path;

    public File(String name) {
        this.name = name;
        this.path = Paths.get(name);
    }

    public int hash() {
        return 123;
    }
}
