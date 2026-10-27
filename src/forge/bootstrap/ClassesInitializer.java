package forge.bootstrap;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import forge.Forge;
import forge.annotations.bootstrap.AnnotationConsumer;
import forge.annotations.bootstrap.Initializer;
import forge.annotations.bootstrap.Static;
import forge.bootstrap.Main.Bootstrap;
import forge.util.Reflex;
import forge.util.io.AtomicFiles;
import forge.util.io.RAF;
import mindustry.Vars;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ClassesInitializer {
    private static final String CLASSES_FILE = "classes.bin";
    
    final Lookup lookup;
    Class<?>[] classes;
    
    private final ClassLoader loader = Vars.mods.mainLoader();
    @Nullable
    private ZipFile zip;
    
    
    ClassesInitializer(Lookup lookup) {
        this.lookup = lookup;
    }
    
    void loadClasses(Bootstrap bootstrap) {
        File file = new File(bootstrap.cacheDir + CLASSES_FILE);
        if (Forge.updated || !tryReadClasses(file)) {
            classesFromZip();
            writeClasses(file);
        }
    }
    
    void initializeClasses() {
        for (Class<?> clazz : classes) {
            tryInitialize(clazz);
        }
    }
    
    private boolean tryReadClasses(File file) {
        try {
            readClasses(file);
            return true;
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException))
                Log.err(e);
            return false;
        }
    }
    
    private void readClasses(File file) throws IOException {
        AtomicFiles.read(file, raf -> {
            classes = Arrays.stream(RAF.readStringArray(raf)).map(this::classForName).toArray(Class[]::new);
        });
    }
    
    private void writeClasses(File file) {
        AtomicFiles.write(file, raf -> {
            RAF.writeStringArray(raf, Arrays.stream(classes).map(Class::getName).toArray(String[]::new));
        });
    }
    
    private void classesFromZip() {
        try (var zip = forgeZipFile()) {
            this.zip = zip;
            classes = extractClassesArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void tryInitialize(Class<?> clazz) {
        try {
            initialize(clazz);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void initialize(Class<?> clazz) throws IllegalAccessException {
        if (Modifier.isPublic(clazz.getModifiers())) {
            lookup.ensureInitialized(clazz);
        } else {
            MethodHandles.privateLookupIn(clazz, lookup)
                .ensureInitialized(clazz);
        }
    }
    
    private Class<?>[] extractClassesArray() {
        var seq = new Seq<Class<?>>();
        extractClasses(seq);
        return toClassesArray(seq);
    }
    
    private void extractClasses(Seq<Class<?>> classes) {
        for (ZipEntry entry : forgePackageEntries()) {
            var clazz = classForName(formatClassName(entry.getName()));
            checkStaticMark(clazz);
            classes.add(clazz);
        }
    }
    
    private Class<?> classForName(String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkStaticMark(Class<?> clazz) {
        if (clazz.getAnnotation(Static.class) != null) {
            tryInitialize(clazz);
        }
    }
    
    private ZipEntry[] forgePackageEntries() {
        return filterForgePackage(zip.stream())
            .toArray(ZipEntry[]::new);
    }
    
    @AnnotationConsumer
    private static void initializerConsumer(Initializer ignored, Method initializer) {
        Reflex.invokePrivateStatic(initializer);
    }
    
    private static ZipFile forgeZipFile() throws IOException {
        try { // jar of this class
            URI uri = ClassesInitializer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            return new ZipFile(Path.of(uri).toFile());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Class<?>[] toClassesArray(Seq<Class<?>> seq) {
        var array = new Class<?>[seq.size];
        System.arraycopy(seq.items, 0, array, 0, seq.size);
        return array;
    }
    
    private static Stream<? extends ZipEntry> filterForgePackage(Stream<? extends ZipEntry> stream) {
        return stream.filter(entry -> {
            String name = entry.getName();
            return name.startsWith("forge/") && name.endsWith(".class");
        });
    }
    
    private static String formatClassName(String name) {
        return name
            .substring(0, name.length() - 6)
            .replace("/", ".");
    }
    
    private static void logIfFileFound(IOException e) {
        if (!(e instanceof FileNotFoundException))
            Log.err(e);
    }
}
