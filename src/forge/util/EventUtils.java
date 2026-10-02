package forge.util;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;

public class EventUtils {
    public static final ObjectMap<Object, Seq<Cons<Object>>> events = ReflectUtils.get(Events.class, "events");

    public static <T> void once(Class<T> type, Runnable task) {
        Events.on(type, new Cons<T>() {
            public void get(T t) {
                task.run();
                Core.app.post(() -> Events.remove(type, this));
            }
        });
    }

    public static void on(Object type, Cons<Object> cons) {
        ArrayUtils.getOrDefault(events, type, Seq::new).add(cons);
    }

    public static void remove(Object type, Cons<Object> cons) {
        events.get(type).remove(cons);
    }

    public static void safeRemove(Object type, Cons<Object> cons) {
        Core.app.post(() -> remove(type, cons));
    }
}
