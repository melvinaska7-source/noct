package polar.ru.api.utils.namespaced;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

public final class FileUtils {
    public static void reset(String str) throws IOException {
        Path path = Paths.get(str, new String[0]);
        if (Files.exists(path, new LinkOption[0])) {
            new File(str).delete();
        }
        Files.createFile(path, new FileAttribute[0]);
    }

    public static boolean exists(String str) {
        return Files.exists(Paths.get(str, new String[0]), new LinkOption[0]);
    }
    private FileUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

