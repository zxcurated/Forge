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
import mindustry.mod.Mod;
import mindustry.mod.Mods.LoadedMod;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

@SuppressWarnings("unused")
public class ForgeBootstrap extends Mod {
    private final Lookup lookup = MethodHandles.lookup();
    
    private final ClassesInitializer initializer = new ClassesInitializer(lookup);
    private final AnnotationsCache annotationsCache = new AnnotationsCache();
    
    private Thread thread;
    
    public ForgeBootstrap() {
        startAsyncLoad();
        clientLoadedListener();
    }
    
    // virtual thread
    private void load() {
        initializer.loadClasses();
        annotationsCache.loadAnnotations(initializer.classes);
    }
    
    // main thread
    private void clientLoaded() {
        initializer.initializeClasses();
        annotationsCache.invokeConsumers();
    }
    
    private void startAsyncLoad() {
        thread = Thread.ofVirtual()
            .name("forge-loading")
            .uncaughtExceptionHandler((t, e) -> Core.app.post(() -> { throw new RuntimeException(e); }))
            .start(this::load);
    }
    
    private void clientLoadedEvent() {
        joinThread();
        replaceModMain();
        clientLoaded();
    }
    
    private void joinThread() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("deprecation")
    private void replaceModMain() {
        try {
            var mod = Vars.mods.getMod("forge");
            var unsafe = (Unsafe) Reflect.get(Unsafe.class, "theUnsafe");
            var field = LoadedMod.class.getDeclaredField("main");
            var offset = unsafe.objectFieldOffset(field);
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
    
    private static void removeClientLoadEvent() {
        Core.app.post(() ->
            Reflect.<ObjectMap<Object, Seq<Cons<?>>>>get(Events.class, "events")
                .remove(ClientLoadEvent.class)
        );
    }
}
