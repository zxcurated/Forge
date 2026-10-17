package forge.bootstrap;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.func.Cons2;
import arc.util.Log;
import forge.util.io.BinReader;
import forge.util.io.BinWriter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

class AnnotationsCache {
    private static final int
        CACHE_VERSION_ERROR = -1,
        CACHE_VERSION = 1;
    
    private static final String
        CACHE_VERSION_FILE = "cache_version.version",
        MEMBERS_POOL_FILE = "pools/members_pool.bin",
        TYPES_POOL_FILE = "pools/types_pool.bin",
        CONSUMERS_POOL_FILE = "pools/consumers_pool.bin",
        ANNOTATION_TYPES_DIR = "types/";
    
    private static final byte
        NOT_IN_POOL = 0,
        IN_TYPES_POOL = 1,
        IN_MEMBERS_POOL = 2,
        IN_CLASSES_POOL = 3,
        IS_TYPE = 0,
        IS_FIELD = 1,
        IS_METHOD = 2;
    
    private final Fi dir = Core.settings.getDataDirectory().child("forge/annotations");
    
    private Class<?>[] classesPool;
    private Object[][] typesPool;
    private Object[] membersPool;
    
    private Method[] consumersPool;
    private Object[][] consumedMembers;
    
    void loadAnnotations(Class<?>[] classes) {
        classesPool = classes;
        if (readCacheVersion() != CACHE_VERSION) {
            deleteCache();
            loadCache();
            writeCache();
        } else {
            readCache();
        }
    }
    
    void invokeConsumers() {
        if (consumersPool != null) {
            int length = consumersPool.length;
            for (int i = 0; i < length; i++) {
                Method consumer = consumersPool[i];
                consumer.setAccessible(true);
                for (var member : consumedMembers[i])
                    invoke(consumer, member);
            }
        }
    }
    
    private void deleteCache() {
        Fi annotationsDir = dir.child("annotations/");
        if (annotationsDir.exists()) {
            annotationsDir.deleteDirectory();
        }
    }
    
    private int readCacheVersion() {
        Fi cacheVersionFile = new Fi(CACHE_VERSION_FILE);
        if (!cacheVersionFile.exists())
            return CACHE_VERSION_ERROR;
        return new BinReader(dir.child(CACHE_VERSION_FILE)).readInt();
    }
    
    private static void invoke(Method consumer, Object member) {
        try {
            consumer.invoke(null, member);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static class AnnotationsCacheException extends RuntimeException {
        private AnnotationsCacheException(String message) {
            super(message);
        }
    }
    
    // region LOAD
    
    private static void loadCache() {
        Log.info("load cache");
    }
    
    // endregion
    // region WRITE
    
    private void writeCache() {
        writeVersion();
        writeTypesPool();
        writeMembersPool();
        writeConsumersPool();
        writeConsumedMembers();
    }
    
    private void writeTypesPool() {
        if (typesPool != null) writeElements(TYPES_POOL_FILE, typesPool.length, this::writeType);
    }
    
    private void writeType(BinWriter writer, int index) {
        var example = typesPool[index][0];
        writer.writeByte(declaringClassIndex(example));
        writer.writeByte(getTypeFlag(example));
    }
    
    private byte declaringClassIndex(Object member) {
        Class<?> clazz = ((Member) member).getDeclaringClass();
        for (byte i = 0; i < classesPool.length; i++)
            if (clazz == classesPool[i])
                return i;
        throw new AnnotationsCacheException("Not found declaring class");
    }
    
    @SuppressWarnings("unused")
    private byte getTypeFlag(Object member) {
        return switch (member) {
            case Class<?> type -> IS_TYPE;
            case Field field -> IS_FIELD;
            case Method method -> IS_METHOD;
            default -> throw new AnnotationsCacheException("Illegal member type");
        };
    }
    
    private void writeMembersPool() {
        if (membersPool != null) writeElements(MEMBERS_POOL_FILE, membersPool.length, this::writeMember);
    }
    
    private void writeConsumersPool() {
        if (consumersPool != null) writeElements(CONSUMERS_POOL_FILE, consumersPool.length, this::writeConsumer);
    }
    
    private void writeConsumer(BinWriter writer, int index) {
        writeMember(writer, consumersPool[index]);
    }
    
    private void writeConsumedMembers() {
        if (consumedMembers != null) {
            int length = consumedMembers.length; // == consumersPool
            for (int index = 0; index < length; index++) {
                writeConsumedArray(index);
            }
        }
    }
    
    private void writeConsumedArray(int index) {
        var annotationType = consumersPool[index].getParameterTypes()[0];
        var consumedArray = consumedMembers[index];
        writeElements(typeFile(annotationType.getSimpleName()), consumedArray.length,
            (writer, i) -> writeMember(writer, consumedArray[i])
        );
    }
    
    private byte[] findInTypes(Object member) {
        var clazz = declaringClass(member);
        for (byte i = 0; i < typesPool.length; i++) {
            if (declaringClass(typesPool[i][0]) == clazz) {
                var typeArray = typesPool[i];
                for (byte j = 0; j < typeArray.length; j++) {
                    if (typeArray[j] == member)
                        // typesPool[i, j];
                        return new byte[]{i, j};
                }
            }
        }
        return null;
    }
    
    private void writeMember(BinWriter writer, Object member) {
        writer.writeByte(getTypeFlag(member));
        if (member instanceof Class<?> clazz) {
            writer.writeByte(classIndex(clazz));
        } else {
            byte[] indexes = findInTypes(member);
            if (indexes != null) {
                writeMemberIndexes(writer, indexes);
            } else {
                writeRawMember(writer, member);
            }
        }
    }
    
    private void writeMemberIndexes(BinWriter writer, byte[] indexes) {
        writer.writeByte(indexes[0]);
        writer.writeByte(indexes[1]);
    }
    
    private void writeRawMember(BinWriter writer, Object member) {
        writer.writeByte(declaringClassIndex(member));
        writer.writeByte(getTypeFlag(member));
        writer.writeInt(member.hashCode());
    }
    
    private byte classIndex(Class<?> clazz) {
        for (byte i = 0; i < classesPool.length; i++)
            if (clazz == classesPool[i])
                return i;
        throw new AnnotationsCacheException("Classes pool index out of bounds");
    }
    
    private static Class<?> declaringClass(Object member) {
        return ((Member) member).getDeclaringClass();
    }
    
    private void writeElements(String file, int length, Cons2<BinWriter, Integer> writerCons) {
        try (var writer = new BinWriter(dir.child(file))) {
            writer.writeInt(length);
            for (int i = 0; i < length; i++)
                writerCons.get(writer, i);
        }
    }
    
    private void writeVersion() {
        try (var writer = new BinWriter(dir.child(CACHE_VERSION_FILE))) {
            writer.writeInt(CACHE_VERSION);
        }
    }
    
    // endregion
    // region READ
    
    private void readCache() {
        readTypesPool();
        readMembersPool();
        readConsumersMap();
    }
    
    private void readTypesPool() {
        readElements(TYPES_POOL_FILE, this::createTypesPool, this::readType);
    }
    
    private void createTypesPool(int length) {
        typesPool = new Object[length][];
    }
    
    private void readType(BinReader reader, int index) {
        Class<?> clazz = classesPool[reader.readByte()];
        typesPool[index] = typeFromClass(reader, clazz);
    }
    
    private void readMembersPool() {
        readElements(MEMBERS_POOL_FILE, this::createMembersPool, this::readMember);
    }
    
    private void createMembersPool(int length) {
        membersPool = new Object[length];
    }
    
    private void readMember(BinReader reader, int index) {
        membersPool[index] = getMember(reader);
    }
    
    private void readConsumersMap() {
        readElements(CONSUMERS_POOL_FILE, this::createConsumersMap, this::readConsumer);
    }
    
    private void createConsumersMap(int length) {
        consumersPool = new Method[length];
        consumedMembers = new Object[length][];
    }
    
    private void readConsumer(BinReader reader, int index) {
        consumersPool[index] = (Method) getMember(reader);
        consumedMembers[index] = readConsumedArray(reader.readString());
    }
    
    // reader of "annotations/types/@{type}.bin"
    private Object[] readConsumedArray(String type) {
        var typeReader = new BinReader(dir.child(typeFile(type)));
        var length = typeReader.readInt();
        var array = new Object[length];
        for (int i = 0; i < length; i++)
            array[i] = getMember(typeReader);
        return array;
    }
    
    private static String typeFile(String type) {
        return ANNOTATION_TYPES_DIR + '@' + type + ".bin";
    }
    
    private void readElements(String file, Cons<Integer> lengthCons, Cons2<BinReader, Integer> readerCons) {
        var reader = new BinReader(dir.child(file));
        var length = reader.readInt();
        lengthCons.get(length);
        for (int i = 0; i < length; i++) {
            readerCons.get(reader, i);
        }
    }
    
    private static Object[] typeFromClass(BinReader reader, Class<?> clazz) throws AnnotationsCacheException {
        return switch (reader.readByte()) {
            case IS_TYPE -> clazz.getDeclaredClasses();
            case IS_FIELD -> clazz.getDeclaredFields();
            case IS_METHOD -> clazz.getDeclaredMethods();
            default -> throw new AnnotationsCacheException("Illegal member type");
        };
    }
    
    private Object getMember(BinReader reader) {
        return switch (reader.readByte()) {
            case NOT_IN_POOL -> findByHash(reader);
            case IN_TYPES_POOL -> fromTypesPool(reader);
            case IN_MEMBERS_POOL -> fromMembersPool(reader);
            case IN_CLASSES_POOL -> fromClassesPool(reader);
            default -> throw new AnnotationsCacheException("Illegal location of member");
        };
    }
    
    private Object findByHash(BinReader reader) {
        Class<?> clazz = classesPool[reader.readByte()];
        Object[] members = typeFromClass(reader, clazz);
        int hashCode = reader.readInt();
        for (Object member : members)
            if (member.hashCode() == hashCode)
                return member;
        throw new AnnotationsCacheException("Not found member with hashCode " + hashCode + " in " + clazz);
    }
    
    private Object fromTypesPool(BinReader reader) {
        return typesPool[reader.readByte()][reader.readByte()];
    }
    
    private Object fromMembersPool(BinReader reader) {
        return membersPool[reader.readByte()];
    }
    
    private Object fromClassesPool(BinReader reader) {
        return classesPool[reader.readByte()];
    }
    
    // endregion
}
