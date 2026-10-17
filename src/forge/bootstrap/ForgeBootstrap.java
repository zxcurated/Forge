package forge.bootstrap;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Reflect;
import forge.Forge;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mods.LoadedMod;

class ForgeBootstrap {
    private final ClassesLoader classesLoader = new ClassesLoader();
    private final AnnotationsCache annotationsCache = new AnnotationsCache();
    
    private boolean isLoaded = false;
    
    ForgeBootstrap() {
        startAsyncLoad();
        clientLoadedListener();
    }
    
    // virtual thread in static block
    private void load() {
        classesLoader.loadClasses();
        annotationsCache.loadAnnotations(classesLoader.classes);
    }
    
    // main thread when client loaded
    private void clientLoaded() {
        classesLoader.initializeClasses();
        annotationsCache.invokeConsumers();
    }
    
    private void startAsyncLoad() {
        Thread.ofVirtual().start(() -> {
            load();
            notifyLock();
        });
    }
    
    private void clientLoadedEvent() {
        waitLock();
        replaceLoadedMod();
        clientLoaded();
    }
    
    @SuppressWarnings("deprecation")
    private void replaceModMain() {
        try {
            var mod = Vars.mods.getMod("forge");
            Field field = LoadedMod.class.getDeclaredField("main");
            Unsafe unsafe = Reflect.get(Unsafe.class, "theUnsafe");
            long offset = unsafe.objectFieldOffset(field);
            unsafe.putObject(mod, offset, new Forge());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void clientLoadedListener() {
        Events.on(ClientLoadEvent.class, e -> {
            clientLoadedEvent();
            removeClientLoadEvent();
        });
    }
    
    private synchronized void notifyLock() {
        isLoaded = true;
        notify();
    }
    
    private synchronized void waitLock() {
        while (!isLoaded) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static LoadedMod copyLoadedMod(LoadedMod orig) {
        return new LoadedMod(orig.file, orig.root, new Forge(), orig.loader, orig.meta);
    }
    
    private static void removeClientLoadEvent() {
        Core.app.post(() ->
            Reflect.<ObjectMap<Object, Seq<Cons<?>>>>get(Events.class, "events")
                .remove(ClientLoadEvent.class)
        );
    }
}
