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
    
    private void replaceLoadedMod() {
        var seq = Vars.mods.list();
        var orig = Vars.mods.getMod(ForgeMod.class);
        seq.remove(orig);
        seq.add(copyLoadedMod(orig));
    }
    
    private void clientLoadedListener() {
        Events.on(ClientLoadEvent.class, e -> {
            clientLoadedEvent();
            removeClientLoadEvent();
        });
    }
    
    private static void removeClientLoadEvent() {
        Core.app.post(() ->
            Reflect.<ObjectMap<Object, Seq<Cons<?>>>>get(Events.class, "events")
                .remove(ClientLoadEvent.class)
        );
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
