package polar.ru.client.modules.impl.misc;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import polar.ru.client.figura.FiguraAvatarManager;

public final class FiguraAvatarInstaller {
    private static final String RESOURCE_ROOT = "figura_avatars";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final AtomicInteger INSTALLED_FILES = new AtomicInteger();
    private static final AtomicInteger SKIPPED_FILES = new AtomicInteger();
    private static final AtomicInteger REPLACED_FILES = new AtomicInteger();
    private static volatile boolean finished;
    private static volatile Throwable lastError;

    private FiguraAvatarInstaller() {
    }

    public static void installAsync() {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                FiguraAvatarInstaller.prepareCounters();
                FiguraAvatarInstaller.installNow();
                finished = true;
                lastError = null;
            }
            catch (Throwable t2) {
                finished = false;
                lastError = t2;
            }
            finally {
                RUNNING.set(false);
            }
        }, "Polar-Figura-Avatar-Installer");
        thread.setDaemon(true);
        thread.start();
    }

    public static void installBlocking() throws Exception {
        if (!RUNNING.compareAndSet(false, true)) {
            FiguraAvatarInstaller.waitForRunningInstall();
            FiguraAvatarInstaller.rethrowLastErrorIfPresent();
            return;
        }
        try {
            FiguraAvatarInstaller.prepareCounters();
            FiguraAvatarInstaller.installNow();
            finished = true;
            lastError = null;
        }
        catch (Throwable t2) {
            finished = false;
            lastError = t2;
            FiguraAvatarInstaller.throwAsException(t2);
        }
        finally {
            RUNNING.set(false);
        }
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static boolean isFinished() {
        return finished;
    }

    public static int getInstalledFiles() {
        return INSTALLED_FILES.get();
    }

    public static int getSkippedFiles() {
        return SKIPPED_FILES.get();
    }

    public static int getReplacedFiles() {
        return REPLACED_FILES.get();
    }

    public static Throwable getLastError() {
        return lastError;
    }

    private static void prepareCounters() {
        finished = false;
        INSTALLED_FILES.set(0);
        SKIPPED_FILES.set(0);
        REPLACED_FILES.set(0);
        lastError = null;
    }

    private static void waitForRunningInstall() throws InterruptedException {
        while (RUNNING.get()) {
            Thread.sleep(10L);
        }
    }

    private static void rethrowLastErrorIfPresent() throws Exception {
        Throwable error = lastError;
        if (error != null) {
            FiguraAvatarInstaller.throwAsException(error);
        }
    }

    private static void throwAsException(Throwable throwable) throws Exception {
        if (throwable instanceof Exception) {
            Exception exception = (Exception)throwable;
            throw exception;
        }
        if (throwable instanceof Error) {
            Error error = (Error)throwable;
            throw error;
        }
        throw new Exception(throwable);
    }

    private static void installNow() throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.runDirectory == null) {
            return;
        }
        Path avatarsDir = mc.runDirectory.toPath().resolve("figura").resolve("avatars").normalize();
        Files.createDirectories(avatarsDir, new FileAttribute[0]);
        ClassLoader loader = FiguraAvatarInstaller.class.getClassLoader();
        Enumeration<URL> roots = loader.getResources(RESOURCE_ROOT);
        boolean found = false;
        while (roots.hasMoreElements()) {
            URL root = roots.nextElement();
            found = true;
            FiguraAvatarInstaller.copyResourceRoot(root, avatarsDir);
        }
        if (!found) {
            FiguraAvatarInstaller.copyFromCodeSourceJar(avatarsDir);
        }
        FiguraAvatarInstaller.repairInstalledAvatars(avatarsDir);
    }

    private static void repairInstalledAvatars(Path avatarsDir) {
        if (!Files.isDirectory(avatarsDir, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.list(avatarsDir);){
            stream.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).forEach(FiguraAvatarManager::repairAvatarMetadata);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static void copyResourceRoot(URL root, Path avatarsDir) throws Exception {
        String protocol = root.getProtocol();
        if ("file".equalsIgnoreCase(protocol)) {
            Path rootPath = Path.of(root.toURI()).normalize();
            FiguraAvatarInstaller.copyDirectory(rootPath, avatarsDir);
            return;
        }
        if ("jar".equalsIgnoreCase(protocol)) {
            JarURLConnection connection = (JarURLConnection)root.openConnection();
            String entryName = connection.getEntryName();
            if (entryName == null || entryName.isEmpty()) {
                entryName = RESOURCE_ROOT;
            }
            try (JarFile jar = connection.getJarFile();){
                FiguraAvatarInstaller.copyFromJar(jar, entryName, avatarsDir);
            }
        }
    }

    private static void copyFromCodeSourceJar(Path avatarsDir) throws Exception {
        URL location = FiguraAvatarInstaller.class.getProtectionDomain().getCodeSource().getLocation();
        if (location == null) {
            return;
        }
        URI uri = location.toURI();
        Path path = Path.of(uri).normalize();
        if (Files.isDirectory(path, new LinkOption[0])) {
            Path root = path.resolve(RESOURCE_ROOT).normalize();
            if (Files.isDirectory(root, new LinkOption[0])) {
                FiguraAvatarInstaller.copyDirectory(root, avatarsDir);
            }
            return;
        }
        if (Files.isRegularFile(path, new LinkOption[0])) {
            try (JarFile jar = new JarFile(path.toFile());){
                FiguraAvatarInstaller.copyFromJar(jar, RESOURCE_ROOT, avatarsDir);
            }
        }
    }

    private static void copyDirectory(Path root, Path avatarsDir) throws IOException {
        if (!Files.isDirectory(root, new LinkOption[0])) {
            return;
        }
        Path normalizedRoot = root.normalize();
        Path normalizedAvatarsDir = avatarsDir.normalize();
        try (Stream<Path> stream = Files.walk(normalizedRoot, new FileVisitOption[0]);){
            stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).forEach(file -> {
                try {
                    Path normalizedFile = file.normalize();
                    Path relativePath = normalizedRoot.relativize(normalizedFile).normalize();
                    if (relativePath.isAbsolute() || FiguraAvatarInstaller.startsWithParentTraversal(relativePath)) {
                        return;
                    }
                    Path output = normalizedAvatarsDir.resolve(relativePath).normalize();
                    FiguraAvatarInstaller.copyFile(normalizedFile, output, normalizedAvatarsDir);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            });
        }
    }

    private static void copyFromJar(JarFile jar, String rootEntry, Path avatarsDir) throws IOException {
        Path normalizedAvatarsDir = avatarsDir.normalize();
        Object prefix = rootEntry.endsWith("/") ? rootEntry : rootEntry + "/";
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            Path output;
            Path relativePath;
            String relativeName;
            String name;
            JarEntry entry = entries.nextElement();
            if (entry == null || entry.isDirectory() || !(name = entry.getName()).startsWith((String)prefix) || (relativeName = name.substring(((String)prefix).length())).isEmpty() || relativeName.contains("\\") || (relativePath = Path.of(relativeName, new String[0]).normalize()).isAbsolute() || FiguraAvatarInstaller.startsWithParentTraversal(relativePath) || !(output = normalizedAvatarsDir.resolve(relativePath).normalize()).startsWith(normalizedAvatarsDir)) continue;
            Files.createDirectories(output.getParent(), new FileAttribute[0]);
            InputStream input = jar.getInputStream(entry);
            try {
                FiguraAvatarInstaller.copyStream(input, output, normalizedAvatarsDir, entry.getSize());
            }
            finally {
                if (input == null) continue;
                input.close();
            }
        }
    }

    private static boolean startsWithParentTraversal(Path path) {
        return path.getNameCount() > 0 && "..".equals(path.getName(0).toString());
    }

    private static void copyFile(Path input, Path output, Path avatarsDir) throws IOException {
        Path normalizedAvatarsDir;
        Path normalizedOutput = output.normalize();
        if (!normalizedOutput.startsWith(normalizedAvatarsDir = avatarsDir.normalize())) {
            return;
        }
        Files.createDirectories(normalizedOutput.getParent(), new FileAttribute[0]);
        long sourceSize = Files.size(input);
        if (Files.exists(normalizedOutput, new LinkOption[0]) && Files.size(normalizedOutput) == sourceSize) {
            SKIPPED_FILES.incrementAndGet();
            return;
        }
        if (Files.exists(normalizedOutput, new LinkOption[0])) {
            REPLACED_FILES.incrementAndGet();
        } else {
            INSTALLED_FILES.incrementAndGet();
        }
        Files.copy(input, normalizedOutput, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static void copyStream(InputStream input, Path output, Path avatarsDir, long sourceSize) throws IOException {
        Path normalizedAvatarsDir;
        Path normalizedOutput = output.normalize();
        if (!normalizedOutput.startsWith(normalizedAvatarsDir = avatarsDir.normalize())) {
            return;
        }
        Files.createDirectories(normalizedOutput.getParent(), new FileAttribute[0]);
        if (Files.exists(normalizedOutput, new LinkOption[0]) && sourceSize >= 0L && Files.size(normalizedOutput) == sourceSize) {
            SKIPPED_FILES.incrementAndGet();
            return;
        }
        if (Files.exists(normalizedOutput, new LinkOption[0])) {
            REPLACED_FILES.incrementAndGet();
        } else {
            INSTALLED_FILES.incrementAndGet();
        }
        Files.copy(input, normalizedOutput, StandardCopyOption.REPLACE_EXISTING);
    }
}

