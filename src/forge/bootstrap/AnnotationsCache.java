package forge.bootstrap;

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import forge.Forge;
import forge.bootstrap.Main.Bootstrap;
import forge.util.Reflex;
import forge.util.io.AtomicFiles;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

class AnnotationsCache {
    private static final String
        POOLS_DIR = "pools/",
        ANNOTATION_TYPES_DIR = "types/",
    
        MEMBERS_POOL_FILE = POOLS_DIR + "members_pool.bin",
        ARRAYS_POOL_FILE = POOLS_DIR + "types_pool.bin",
        CONSUMERS_POOL_FILE = POOLS_DIR + "consumers_pool.bin";
    
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

    private String cacheDir;
    
    private Class<?>[] classesPool;
    private Member[][] typesPool;
    private Member[] membersPool;
    
    private Method[] consumersPool;
    private Annotation[][] consumedAnnotations;
    private Object[][] consumedElements;
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void loadAnnotations(Bootstrap bootstrap, Class<?>[] classes) {
        classesPool = classes;
        
        cacheDir = bootstrap.cacheDir ;
        new File(bootstrap.cacheDir + ANNOTATION_TYPES_DIR).mkdirs();
        new File(bootstrap.cacheDir + POOLS_DIR).mkdirs();
        
        if (!Forge.updated) {
            new CacheReader();
        } else {
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
                    Reflex.invokeStatic(consumer, consumedAnnotations[j], elements[j]);
            }
        }
    }
    
    private File file(String name) {
        return new File(cacheDir + name);
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
            var value = Reflex.invoke(method, annotation);
            var def = method.getDefaultValue();
            if (!value.equals(def))
                return false;
        }
        return true;
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
        // not found
        return null;
    }
    
    private int findInMembersPool(Object member) {
        for (int i = 0; i < membersPool.length; i++)
            if (member == membersPool[i])
                return i;
        // not found
        return -1;
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
        // visible and can be instanced only in AnnotationsCache class
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
        }
        
        private void parse() {
            for (Class<?> clazz : classesPool) {
                countAnnotations(clazz, clazz.getAnnotations());
                countUsages(clazz.getDeclaredFields(), FIELD);
                countUsages(clazz.getDeclaredMethods(), METHOD);
            }
        }
        
        private void setup() {
            //todo перенос и форматирование данных из временных в поля кэша
        }
        
        private void countUsages(AnnotatedElement[] members, ElementType type) {
            int usages = 0;
            //todo я забыл что сломал тут, хз
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
    
    private interface ReadWriteConsumer {
        void get(RandomAccessFile raf, int index) throws IOException;
    }
    
    private class CacheWriter {
        
        private CacheWriter() throws IOException {
            makeDirs();
            writeTypesPool();
            writeMembersPool();
            writeConsumersPool();
            writeConsumedMembers();
        }
        
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void makeDirs() {
            new File(POOLS_DIR).mkdirs();
            new File(ANNOTATION_TYPES_DIR).mkdirs();
        }
        
        private void writeElements(String fileName, int length, ReadWriteConsumer cons) throws IOException {
            AtomicFiles.write(file(fileName), raf -> {
                raf.writeInt(length);
                for (int i = 0; i < length; i++)
                    cons.get(raf, i);
            });
        }
        
        private void writeElement(RandomAccessFile raf, Object element) throws IOException {
            if (element instanceof Class<?> clazz) {
                raf.writeByte(IN_CLASSES_POOL);
                raf.writeByte(classIndex(clazz));
            } else {
                int[] indexes = findInTypesPool(element);
                if (indexes != null) {
                    raf.writeByte(IN_ARRAYS_POOL);
                    writeMemberIndexes(raf, indexes);
                } else {
                    int index = findInMembersPool(element);
                    if (index != -1) {
                        raf.writeByte(IN_MEMBERS_POOL);
                        raf.writeByte(index);
                    } else {
                        raf.writeByte(NOT_IN_POOL);
                        writeRawMember(raf, element);
                    }
                }
            }
        }
        
        private void writeMemberIndexes(RandomAccessFile raf, int[] indexes) throws IOException {
            raf.writeByte(indexes[0]);
            raf.writeByte(indexes[1]);
        }
        
        private void writeRawMember(RandomAccessFile raf, Object member) throws IOException {
            raf.writeByte(declaringClassIndex(member));
            raf.writeByte(getTypeFlag(member));
            raf.writeInt(member instanceof Method method ? methodHashCode(method) : member.hashCode());
        }
        
        // region types pool
        
        private void writeTypesPool() throws IOException {
            if (typesPool != null) {
                writeElements(ARRAYS_POOL_FILE, typesPool.length, this::writeType);
            }
        }
        
        private void writeType(RandomAccessFile raf, int index) throws IOException {
            var example = typesPool[index][0];
            raf.writeByte(declaringClassIndex(example));
            raf.writeByte(getTypeFlag(example));
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
        
        private void writeConsumer(RandomAccessFile raf, int index) throws IOException {
            writeElement(raf, consumersPool[index]);
            raf.writeByte(classIndex(consumerType(consumersPool[index])));
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
        private void writeConsumedArray(int arrayIndex) throws IOException {
            var annotationType = asAnnotationType(consumersPool[arrayIndex].getParameterTypes()[0]);
            var consumedArray = consumedElements[arrayIndex];
            writeElements(arrayFile(annotationType.getSimpleName()), consumedArray.length,
                (raf, elementIndex) -> {
                    writeElement(raf, consumedArray[elementIndex]);
                    writeAnnotationStatus(raf, consumedArray[elementIndex], annotationType);
                }
            );
        }
        
        private void writeAnnotationStatus(RandomAccessFile raf, Object element, Class<? extends Annotation> type) throws IOException {
            var annotated = (AnnotatedElement) element;
            var annotation = annotated.getAnnotation(type);
            raf.writeByte(isDefaultValues(annotation, type) ? IS_EMPTY_ANNOTATION : NOT_EMPTY_ANNOTATION);
        }
        
        // endregion consumed members
    }
    
    private class CacheReader {
        
        private CacheReader() {
            try {
                readTypesPool();
                readMembersPool();
                readConsumersMap();
            } catch (IOException e) {
                Log.err(e);
                writeCache();
            }
        }
        
        private void readElements(String fileName, Cons<Integer> lengthCons, ReadWriteConsumer cons) throws IOException {
            AtomicFiles.read(file(fileName), true, raf -> {
                var length = raf.readInt();
                lengthCons.get(length);
                for (int i = 0; i < length; i++) {
                    cons.get(raf, i);
                }
            });
        }
        
        private Object readElement(RandomAccessFile raf) throws IOException {
            return switch (raf.readByte()) {
                case NOT_IN_POOL -> readByHash(raf);
                case IN_ARRAYS_POOL -> typesPool[raf.readInt()][raf.readInt()];
                case IN_MEMBERS_POOL -> membersPool[raf.readInt()];
                case IN_CLASSES_POOL -> classesPool[raf.readInt()];
                default -> throw new AnnotationsCacheException("Illegal location of element");
            };
        }
        
        private static Member[] readTypeFromClass(RandomAccessFile raf, Class<?> clazz) throws AnnotationsCacheException, IOException {
            return switch (raf.readByte()) {
                case IS_FIELD -> clazz.getDeclaredFields();
                case IS_METHOD -> clazz.getDeclaredMethods();
                default -> throw new AnnotationsCacheException("Illegal member type");
            };
        }
        
        private Object readByHash(RandomAccessFile raf) throws IOException {
            Class<?> clazz = classesPool[raf.readInt()];
            int type = raf.readByte();
            int hashCode = raf.readInt();
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
        
        private void readType(RandomAccessFile raf, int index) throws IOException {
            Class<?> clazz = classesPool[raf.readInt()];
            typesPool[index] = readTypeFromClass(raf, clazz);
        }
        
        // endregion types pool
        // region members pool
        
        private void readMembersPool() throws IOException {
            readElements(MEMBERS_POOL_FILE, this::createMembersPool, this::readPoolMember);
        }
        
        private void createMembersPool(int length) {
            membersPool = new Member[length];
        }
        
        private void readPoolMember(RandomAccessFile raf, int index) throws IOException {
            membersPool[index] = (Member) readElement(raf);
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
        
        private void readConsumer(RandomAccessFile raf, int index) throws IOException {
            consumersPool[index] = (Method) readElement(raf);
            var annotationType = asAnnotationType(classesPool[raf.readInt()]);
            readConsumedArray(annotationType, index);
        }
        
        // endregion consumers pool
        // region consumed members
        
        // reader of "annotations/types/@{type.name}.bin"
        private void readConsumedArray(Class<? extends Annotation> type, int index) throws IOException {
            AtomicFiles.read(file(arrayFile(type.getSimpleName())), raf -> {
                var length = raf.readInt();
                consumedElements[index] = new Object[length];
                consumedAnnotations[index] = new Annotation[length];
                for (int i = 0; i < length; i++) {
                    var element = readElement(raf);
                    consumedElements[index][i] = element;
                    if (raf.readByte() != IS_EMPTY_ANNOTATION) {
                        consumedAnnotations[index][i] = getAnnotation(element, type);
                    }
                }
            });
        }
        
        // endregion consumed members
    }
}
