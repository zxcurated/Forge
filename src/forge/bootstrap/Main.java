package forge.bootstrap;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Reflect;
import forge.Forge;
import forge.util.Reflex;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;
import mindustry.mod.Mods.LoadedMod;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

@SuppressWarnings("unused")
public class Main extends Mod {
    
    static {
        new Bootstrap();
    }
    
    static class Bootstrap {
        private final Lookup lookup = MethodHandles.lookup();
        
        private final ClassesInitializer initializer = new ClassesInitializer(lookup);
        private final AnnotationsCache annotationsCache = new AnnotationsCache();
        
        private Thread thread;
        public String cacheDir;
        
        public Bootstrap() {
            startAsyncLoad();
            clientLoadedListener();
        }
        
        // virtual thread
        private void load() {
            initCacheDir();
            initializer.loadClasses(this);
            annotationsCache.loadAnnotations(this, initializer.classes);
        }
        
        private void initCacheDir() {
            cacheDir = Core.settings.getDataDirectory() + "/forge/cache/";
            if (Forge.updated) {
                new Fi(cacheDir).emptyDirectory();
            } else {
                //noinspection ResultOfMethodCallIgnored
                new File(cacheDir).mkdirs();
            }
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
                // and update data like synchronized block
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        
        private void replaceModMain() {
            Reflex.setFinal(LoadedMod.class, "main", Vars.mods.getMod("forge"), new Forge());
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
}
