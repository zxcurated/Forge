package forge.bootstrap;

import arc.struct.Seq;
import forge.annotations.bootstrap.Static;

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
    final Lookup lookup;
    Class<?>[] classes;
    
    ClassesLoader(Lookup lookup) {
        this.lookup = lookup;
    }
    
    void loadClasses() {
        try (ZipFile zip = forgeZipFile()) {
            classes = classesArray(zip);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    void initializeClasses() {
        for (Class<?> clazz : classes) {
            try {
                initialize(clazz);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
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
    
    private Class<?>[] classesArray(ZipFile zip) {
        var seq = new Seq<Class<?>>();
        extractClasses(zip, seq);
        return toClassesArray(seq);
    }
    
    private void extractClasses(ZipFile zip, Seq<Class<?>> classes) {
        ClassLoader loader = ClassesLoader.class.getClassLoader();
        for (ZipEntry entry : forgePackageEntries(zip)) {
            var clazz = classForName(entry.getName(), loader);
            initializeIfStatic(clazz);
            classes.add(clazz);
        }
    }
    
    private Class<?> classForName(String name, ClassLoader loader) {
        try {
            return Class.forName(formatClassName(name), false, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeIfStatic(Class<?> clazz) {
        if (clazz.getAnnotation(Static.class) != null) {
            try {
                initialize(clazz);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static ZipFile forgeZipFile() throws IOException {
        try { // jar of this class
            URI uri = ClassesLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI();
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
    
    private static String formatClassName(String name) {
        return name
            .substring(0, name.length() - 6)
            .replace("/", ".");
    }
}
