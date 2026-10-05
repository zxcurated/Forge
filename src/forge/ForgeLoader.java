package forge;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import forge.util.ArrayUtils;
import forge.util.EventUtils;
import forge.util.ReflectUtils;
import forge.util.ThreadUtils;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ForgeLoader extends Mod {

    static {
        Initializer initializer = new Initializer();
        ThreadUtils.virtual(initializer::load);
        EventUtils.once(ClientLoadEvent.class, initializer::initAll);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface AnnotationConsumer {
    }

    private static class Initializer {
        private final Seq<Class<?>> classes = new Seq<>();
        private final ObjectMap<Class<?>, Seq<AnnotatedRecord>> annotatedMap = new ObjectMap<>();
        private final Seq<ConsumerRecord> consumers = new Seq<>();

        private synchronized void load() {
            ClassLoader loader = Vars.mods.mainLoader();
            try (ZipFile zip = new ZipFile(URLDecoder.decode(ForgeLoader.class.getProtectionDomain().getCodeSource().getLocation().getFile(), StandardCharsets.UTF_8))) {
                var iterator = zip.stream().iterator();
                // skip for classes dir
                while (!iterator.next().getName().equals("forge/"));
                ZipEntry entry;
                String name;
                do {
                    entry = iterator.next();
                    name = entry.getName();
                    if (name.endsWith(".class")) {
                        try {
                            // "dir/dir/ClassName.class" -> "dir.dir.ClassName"
                            var clazz = Class.forName(name.substring(0, name.length() - 6).replace('/', '.'), false, loader);
                            if(!clazz.isRecord()) classes.add(clazz);
                        } catch (ClassNotFoundException e) {
                            throw new Error(e);
                        }
                    }
                } while (name.startsWith("forge/"));
            } catch (IOException e) {
                throw new Error(e);
            }

            classes.each(clazz -> {
                extractAnnotated(clazz);
                extractAnnotated(clazz.getDeclaredFields());
                extractAnnotated(clazz.getDeclaredClasses());
                // extract annotated methods and consumers
                for (Method method : clazz.getDeclaredMethods()) {
                    for (Annotation annotation : method.getAnnotations()) {
                        var type = annotation.annotationType();
                        if (type.getName().startsWith("forge.")) {
                            method.setAccessible(true);
                            var types = method.getParameterTypes();
                            if (types.length > 0 && types[0].isAnnotation()) {
                                consumers.add(new ConsumerRecord(types[0], method));
                            } else {
                                ArrayUtils.getOrDefault(annotatedMap, type, Seq::new)
                                    .add(new AnnotatedRecord(annotation, method));
                            }
                        }
                    }
                }
            });
        }

        private synchronized void initAll() {
            try {
                for (Class<?> clazz : classes) {
                    if (!clazz.getSimpleName().isBlank())
                            MethodHandles.lookup().ensureInitialized(clazz);
                }
            } catch (IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }

            for (var consumer : consumers) {
                var seq = annotatedMap.get(consumer.type);
                if(seq != null) {
                    seq.each(annotated -> ReflectUtils.invoke(consumer.method, annotated.annotation, annotated.object));
                }
            }
        }

        private void extractAnnotated(AnnotatedElement... elements) {
            for (AnnotatedElement element : elements) {
                for (Annotation annotation : element.getAnnotations()) {
                    var type = annotation.annotationType();
                    if (type.getName().startsWith("forge."))
                        ArrayUtils.getOrDefault(annotatedMap, type, Seq::new)
                            .add(new AnnotatedRecord(annotation, element));
                }
            }
        }

        private record AnnotatedRecord(Annotation annotation, Object object) {
        }

        private record ConsumerRecord(Class<?> type, Method method) {
        }
    }
}
