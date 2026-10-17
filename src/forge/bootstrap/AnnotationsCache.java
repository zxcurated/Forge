package forge.bootstrap;

import java.lang.reflect.Member;

class AnnotationsCache {
    private Class<?>[] classesPool;
    private Member[][] typesPool;
    private Member[] membersPool;
    
    void loadAnnotations(Class<?>[] classes) {
        classesPool = classes;
    }
    
    void invokeConsumers() {
    
    }
    
    private class CacheWriter {
    
    }
    
    private class CacheReader {
    
    }
}
