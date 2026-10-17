package forge.bootstrap;

import arc.struct.Seq;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ClassesLoader {
    Class<?>[] classes;
    
    void loadClasses() {
        try (ZipFile zip = forgeZipFile()) {
            classes = classesArray(zip);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    void initializeClasses() {
        Lookup lookup = MethodHandles.lookup();
        for (Class<?> clazz : classes) {
            try {
                initialize(clazz, clazz.getModifiers(), lookup);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
   
    private void initialize(Class<?> clazz, int mod, Lookup lookup) throws IllegalAccessException {
        if (!Modifier.isPublic(mod)) {
            lookup = MethodHandles.privateLookupIn(clazz, lookup);
        }
        lookup.ensureInitialized(clazz);
    }
    
    private static ZipFile forgeZipFile() throws IOException {
        try { // jar of this class
            URI uri = ClassesLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            return new ZipFile(Path.of(uri).toFile());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Class<?>[] classesArray(ZipFile zip) {
        var seq = new Seq<Class<?>>();
        extractClasses(zip, seq);
        return toClassesArray(seq);
    }
    
    private static Class<?>[] toClassesArray(Seq<Class<?>> seq) {
        var array = new Class<?>[seq.size];
        System.arraycopy(seq.items, 0, array, 0, seq.size);
        return array;
    }
    
    private static void extractClasses(ZipFile zip, Seq<Class<?>> classes) {
        ClassLoader loader = ClassesLoader.class.getClassLoader();
        for (ZipEntry entry : forgePackageEntries(zip)) {
            classes.add(getClass(entry.getName(), loader));
        }
    }
    
    private static ZipEntry[] forgePackageEntries(ZipFile zip) {
        return filterForgePackage(zip.stream())
            .toArray(ZipEntry[]::new);
    }
    
    private static Stream<? extends ZipEntry> filterForgePackage(Stream<? extends ZipEntry> stream) {
        return stream.filter(entry -> {
            String name = entry.getName();
            return name.startsWith("forge/") && name.endsWith(".class");
        });
    }
    
    private static Class<?> getClass(String name, ClassLoader loader) {
        try {
            return Class.forName(formatClassName(name), false, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String formatClassName(String name) {
        return name
            .substring(0, name.length() - 6)
            .replace("/", ".");
    }
}
