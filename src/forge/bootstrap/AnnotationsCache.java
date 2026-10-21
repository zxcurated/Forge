package forge.bootstrap;

import arc.Core;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class AnnotationsCache {
    private static final String CACHE_VERSION = "0.1";
    
    private static final String
        CACHE_VERSION_FILE = "cache_version.version",
        CACHE_VERSION_FILE_TEMP = CACHE_VERSION_FILE + ".temp",
        
        POOLS_DIR = "pools/",
        ANNOTATION_TYPES_DIR = "types/",
    
        MEMBERS_POOL_FILE = POOLS_DIR + "members_pool.bin",
        ARRAYS_POOL_FILE = POOLS_DIR + "types_pool.bin",
        CONSUMERS_POOL_FILE = POOLS_DIR + "consumers_pool.bin";
    
    private final String cacheDir = Core.settings.getDataDirectory().absolutePath() + "/forge/annotations/";
    
    private static final byte
        NOT_IN_POOL = 0,
        IN_ARRAYS_POOL = 1,
        IN_MEMBERS_POOL = 2,
        IN_CLASSES_POOL = 3,

        IS_FIELD = 0,
        IS_METHOD = 1,
        IS_TYPE = 2,

        IS_EMPTY_ANNOTATION = 0,
        NOT_EMPTY_ANNOTATION = 1;
    
    private Class<?>[] classesPool;
    private Member[][] typesPool;
    private Member[] membersPool;
    
    private Method[] consumersPool;
    private Annotation[][] consumedAnnotations;
    private Object[][] consumedElements;
    
    void loadAnnotations(Class<?>[] classes) {
        classesPool = classes;
        
        try {
            if (isCurrentVersion()) {
                new CacheReader(); // throws IOException
            } else {
                writeCache();
            }
        } catch (IOException e) {
            Log.err(e);
            writeCache();
        }
    }
    
    void invokeConsumers() {
        if (consumersPool != null) {
            int length = consumersPool.length;
            for (int i = 0; i < length; i++) {
                Method consumer = consumersPool[i];
                Object[] elements = consumedElements[i];
                consumer.setAccessible(true);
                for (int j = 0; j < consumedElements.length; j++)
                    invoke(consumer, new Object[]{consumedAnnotations[j], elements[j]});
            }
        }
    }
    
    private File file(String name) {
        return new File(cacheDir + name);
    }
    
    private static void invoke(Method consumer, Object member) {
        try {
            consumer.invoke(null, member);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    private boolean isCurrentVersion() throws IOException {
        return CACHE_VERSION.equals(readCacheVersionBytes());
    }
    
    private String readCacheVersionBytes() throws IOException {
        Path path = file(CACHE_VERSION_FILE).toPath();
        return Files.readString(path);
    }
    
    private void writeCache() {
        try {
            new CacheInitializer();
            new CacheWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Annotation getAnnotation(Object element, Class<? extends Annotation> type) {
        return ((AnnotatedElement) element).getAnnotation(type);
    }
    
    private static String arrayFile(String type) {
        return ANNOTATION_TYPES_DIR + '@' + type + ".bin";
    }
    
    private byte declaringClassIndex(Object member) {
        Class<?> clazz = ((Member) member).getDeclaringClass();
        for (byte i = 0; i < classesPool.length; i++)
            if (clazz == classesPool[i])
                return i;
        
        throw new AnnotationsCacheException("Not found declaring class");
    }
    
    @SuppressWarnings("unused")
    private byte getTypeFlag(Object element) {
        return switch (element) {
            case Class<?> type -> IS_TYPE;
            case Field field -> IS_FIELD;
            case Method method -> IS_METHOD;
            default -> throw new AnnotationsCacheException("Illegal element type");
        };
    }
    
    private boolean isDefaultValues(Annotation annotation, Class<? extends Annotation> type) {
        for (Method method : type.getDeclaredMethods()) {
            var value = methodValue(annotation, method);
            var def = method.getDefaultValue();
            if (!value.equals(def))
                return false;
        }
        return true;
    }
    
    private Object methodValue(Annotation annotation, Method method) {
        try {
            return method.invoke(annotation);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    private int[] findInTypesPool(Object member) {
        var clazz = declaringClass(member);
        for (int i = 0; i < typesPool.length; i++) {
            if (declaringClass(typesPool[i][0]) == clazz) {
                var typeArray = typesPool[i];
                for (int j = 0; j < typeArray.length; j++)
                    if (typeArray[j] == member)
                        // typesPool[i, j];
                        return new int[]{i, j};
            }
        }
        return null;
    }
    
    private int findInMembersPool(Object member) {
        for (int i = 0; i < membersPool.length; i++)
            if (member == membersPool[i])
                return i;
        
        return 0;
    }
    
    private int classIndex(Class<?> clazz) {
        for (int i = 0; i < classesPool.length; i++)
            if (clazz == classesPool[i])
                return i;
        
        throw new AnnotationsCacheException("Classes pool index out of bounds");
    }
    
    private static Class<?> declaringClass(Object member) {
        return ((Member) member).getDeclaringClass();
    }
    
    private Class<? extends Annotation> consumerType(Method consumer) {
        return asAnnotationType(consumer.getParameterTypes()[0]);
    }
    
    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> asAnnotationType(Class<?> clazz) {
        return (Class<? extends Annotation>) clazz;
    }
    
    private Object findFieldByHash(Object[] fields, int hashCode) throws AnnotationsCacheException {
        for (var field : fields)
            if (field.hashCode() == hashCode)
                return field;
        
        throw new AnnotationsCacheException("Not found field with hashCode " + hashCode);
    }
    
    private Object findMethodByHash(Object[] methods, int hashCode) throws AnnotationsCacheException {
        for (var method : methods)
            if (hashCode == methodHashCode(method))
                return method;
        
        throw new AnnotationsCacheException("Not found method with hashCode " + hashCode);
    }
    
    private int methodHashCode(Object method) {
        return method.hashCode() ^ Arrays.hashCode(
            ((Method) method).getParameterTypes()
        );
    }
    
    private static class AnnotationsCacheException extends RuntimeException {
        private AnnotationsCacheException(String message) {
            super(message);
        }
    }
    
    private class CacheInitializer {
        // [class_index][type_flag] - only field or method types
        private final int[][] typesUsages = new int[classesPool.length][];
        private int typesCached = 0;
        
        // members and their usages count
        private final ObjectMap<Object, Integer> membersUsagesMap = new ObjectMap<>();
        // annotation - annotated members
        private final ObjectMap<Annotation, Seq<Object>> annotatedMembers = new ObjectMap<>();
        
        private CacheInitializer() {
            parse();
            setup();
            //todo я уже забыл че надо но тут чето не так, в parse не считает а сетап не готов.
        }
        
        private void parse() {
            for (Class<?> clazz : classesPool) {
                countAnnotations(clazz, clazz.getAnnotations());
                countUsages(clazz.getDeclaredFields());
                countUsages(clazz.getDeclaredMethods());
            }
        }
        
        private void setup() {
            typesPool = new Member[typesCached][];
        }
        
        private void countUsages(AnnotatedElement... members) {
            int usages = 0;
            for (AnnotatedElement member : members)
                if (memberUses(member))
                    usages++;
            
            
        }
        
        private boolean memberUses(AnnotatedElement member) {
            int usages = countAnnotations(member, member.getAnnotations());
            if (usages > 0) {
                membersUsagesMap.put(member, usages);
                return true;
            } else {
                return false;
            }
        }
        
        private int countAnnotations(Object element, Annotation[] annotations) {
            int count = 0;
            for (var annotation : annotations) {
                if (annotation.annotationType().getName().startsWith("forge.")) {
                    saveAnnotated(element, annotation);
                    count++;
                }
            }
            return count;
        }
        
        private void saveAnnotated(Object member, Annotation annotation) {
            var seq = annotatedMembers.get(annotation);
            if (seq == null) {
                seq = new Seq<>(1);
                annotatedMembers.put(annotation, seq);
            }
            seq.add(member);
        }
    }
    
    private interface RAFConsumer {
        void get(RandomAccessFile file, int index) throws IOException;
    }
    
    private class CacheWriter {
        
        private CacheWriter() throws IOException {
            makeDirs();
            writeTypesPool();
            writeMembersPool();
            writeConsumersPool();
            writeConsumedMembers();
            writeVersion();
        }
        
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void makeDirs() {
            new File(POOLS_DIR).mkdirs();
            new File(ANNOTATION_TYPES_DIR).mkdirs();
        }
        
        private void writeElements(String fileName, int length, RAFConsumer rafCons) throws IOException {
            try (var file = new RandomAccessFile(file(fileName), "rw")) {
                file.writeInt(length);
                for (int i = 0; i < length; i++)
                    rafCons.get(file, i);
            }
        }
        
        private void writeElement(RandomAccessFile file, Object element) throws IOException {
            if (element instanceof Class<?> clazz) {
                file.writeByte(IN_CLASSES_POOL);
                file.writeByte(classIndex(clazz));
            } else {
                int[] indexes = findInTypesPool(element);
                if (indexes != null) {
                    file.writeByte(IN_ARRAYS_POOL);
                    writeMemberIndexes(file, indexes);
                } else {
                    int index = findInMembersPool(element);
                    if (index != 0) {
                        file.writeByte(IN_MEMBERS_POOL);
                        file.writeByte(index);
                    } else {
                        file.writeByte(NOT_IN_POOL);
                        writeRawMember(file, element);
                    }
                }
            }
        }
        
        private void writeMemberIndexes(RandomAccessFile file, int[] indexes) throws IOException {
            file.writeByte(indexes[0]);
            file.writeByte(indexes[1]);
        }
        
        private void writeRawMember(RandomAccessFile file, Object member) throws IOException {
            file.writeByte(declaringClassIndex(member));
            file.writeByte(getTypeFlag(member));
            file.writeInt(member instanceof Method method ? methodHashCode(method) : member.hashCode());
        }
        
        private void writeVersion() throws IOException {
            File temp = file(CACHE_VERSION_FILE_TEMP);
            File file = file(CACHE_VERSION_FILE);
            try (var raf = new RandomAccessFile(temp, "rw")) {
                raf.writeUTF(CACHE_VERSION);
            }
            Files.move(file.toPath(), temp.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
        }
        
        // region types pool
        
        private void writeTypesPool() throws IOException {
            if (typesPool != null) {
                writeElements(ARRAYS_POOL_FILE, typesPool.length, this::writeType);
            }
        }
        
        private void writeType(RandomAccessFile file, int index) throws IOException {
            var example = typesPool[index][0];
            file.writeByte(declaringClassIndex(example));
            file.writeByte(getTypeFlag(example));
        }
        
        // endregion types pool
        // region members pool
        
        private void writeMembersPool() throws IOException {
            if (membersPool != null) {
                writeElements(MEMBERS_POOL_FILE, membersPool.length, this::writeType);
            }
        }
        
        // endregion members pool
        // region consumers pool
        
        private void writeConsumersPool() throws IOException {
            if (consumersPool != null) {
                writeElements(CONSUMERS_POOL_FILE, consumersPool.length, this::writeConsumer);
            }
        }
        
        private void writeConsumer(RandomAccessFile file, int index) throws IOException {
            writeElement(file, consumersPool[index]);
            file.writeByte(classIndex(consumerType(consumersPool[index])));
        }
        
        // endregion consumers pool
        // region consumed members
        
        private void writeConsumedMembers() throws IOException {
            if (consumedElements != null) {
                int length = consumedElements.length; // == consumersPool.length
                for (int index = 0; index < length; index++) {
                    writeConsumedArray(index);
                }
            }
        }
        
        // writer of "annotations/types/@{type.name}.bin"
        private void writeConsumedArray(int index) throws IOException {
            var annotationType = asAnnotationType(consumersPool[index].getParameterTypes()[0]);
            var consumedArray = consumedElements[index];
            writeElements(arrayFile(annotationType.getSimpleName()), consumedArray.length,
                (file, i) -> {
                    writeElement(file, consumedArray[i]);
                    writeAnnotationStatus(file, consumedArray[i], annotationType);
                }
            );
        }
        
        private void writeAnnotationStatus(RandomAccessFile file, Object element, Class<? extends Annotation> type) throws IOException {
            var annotated = (AnnotatedElement) element;
            var annotation = annotated.getAnnotation(type);
            file.writeByte(isDefaultValues(annotation, type) ? IS_EMPTY_ANNOTATION : NOT_EMPTY_ANNOTATION);
        }
        
        // endregion consumed members
    }
    
    private class CacheReader {
        
        private CacheReader() throws IOException {
            readTypesPool();
            readMembersPool();
            readConsumersMap();
        }
        
        private void readElements(String fileName, Cons<Integer> lengthCons, RAFConsumer rafCons) throws IOException {
            var file = new RandomAccessFile(file(fileName), "r");
            var length = file.readInt();
            lengthCons.get(length);
            for (int i = 0; i < length; i++) {
                rafCons.get(file, i);
            }
        }
        
        private Object readElement(RandomAccessFile file) throws IOException {
            return switch (file.readByte()) {
                case NOT_IN_POOL -> readByHash(file);
                case IN_ARRAYS_POOL -> typesPool[file.readUnsignedByte()][file.readUnsignedByte()];
                case IN_MEMBERS_POOL -> membersPool[file.readUnsignedByte()];
                case IN_CLASSES_POOL -> classesPool[file.readUnsignedByte()];
                default -> throw new AnnotationsCacheException("Illegal location of element");
            };
        }
        
        private static Member[] readTypeFromClass(RandomAccessFile file, Class<?> clazz) throws AnnotationsCacheException, IOException {
            return switch (file.readByte()) {
                case IS_FIELD -> clazz.getDeclaredFields();
                case IS_METHOD -> clazz.getDeclaredMethods();
                default -> throw new AnnotationsCacheException("Illegal member type");
            };
        }
        
        private Object readByHash(RandomAccessFile file) throws IOException {
            Class<?> clazz = classesPool[file.readUnsignedByte()];
            int type = file.readByte();
            int hashCode = file.readInt();
            return type == IS_METHOD
                ? findMethodByHash(clazz.getDeclaredMethods(), hashCode)
                : findFieldByHash(clazz.getDeclaredFields(), hashCode);
        }
        
        // region types pool
        
        private void readTypesPool() throws IOException {
            readElements(ARRAYS_POOL_FILE, this::createTypesPool, this::readType);
        }
        
        private void createTypesPool(int length) {
            typesPool = new Member[length][];
        }
        
        private void readType(RandomAccessFile file, int index) throws IOException {
            Class<?> clazz = classesPool[file.readUnsignedByte()];
            typesPool[index] = readTypeFromClass(file, clazz);
        }
        
        // endregion types pool
        // region members pool
        
        private void readMembersPool() throws IOException {
            readElements(MEMBERS_POOL_FILE, this::createMembersPool, (file, index) -> {
                try {
                    readPoolMember(file, index);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        private void createMembersPool(int length) {
            membersPool = new Member[length];
        }
        
        private void readPoolMember(RandomAccessFile file, int index) throws IOException {
            membersPool[index] = (Member) readElement(file);
        }
        
        // endregion members pool
        // region consumers pool
        
        private void readConsumersMap() throws IOException {
            readElements(CONSUMERS_POOL_FILE, this::createConsumersMap, this::readConsumer);
        }
        
        private void createConsumersMap(int length) {
            consumersPool = new Method[length];
            consumedElements = new Object[length][];
            consumedAnnotations = new Annotation[length][];
        }
        
        private void readConsumer(RandomAccessFile file, int index) throws IOException {
            consumersPool[index] = (Method) readElement(file);
            var annotationType = asAnnotationType(classesPool[file.readUnsignedByte()]);
            readConsumedArray(annotationType, index);
        }
        
        // endregion consumers pool
        // region consumed members
        
        // reader of "annotations/types/@{type.name}.bin"
        private void readConsumedArray(Class<? extends Annotation> type, int index) throws IOException {
            var file = new RandomAccessFile(file(arrayFile(type.getSimpleName())), "r");
            var length = file.readInt();
            consumedElements[index] = new Object[length];
            consumedAnnotations[index] = new Annotation[length];
            for (int i = 0; i < length; i++) {
                var element = readElement(file);
                consumedElements[index][i] = element;
                if (file.readByte() != IS_EMPTY_ANNOTATION) {
                    consumedAnnotations[index][i] = getAnnotation(element, type);
                }
            }
        }
        
        // endregion consumed members
    }
}
