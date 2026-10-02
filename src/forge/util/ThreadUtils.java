package forge.util;

import arc.util.OS;

import static mindustry.game.EventType.Trigger;

import java.util.concurrent.*;

public class ThreadUtils {
    private static final ConcurrentLinkedDeque<Runnable> queue = new ConcurrentLinkedDeque<>();
    private static volatile boolean queueHaveTasks = false;

    static {
        EventUtils.on(Trigger.update, ThreadUtils::update);
    }

    private static void update(Object ignored) {
        if (queueHaveTasks) {
            queueHaveTasks = false;
            for (Runnable task; (task = queue.poll()) != null; task.run()) ;
        }
    }

    public static void main(Runnable task) {
        queue.add(task);
        queueHaveTasks = true;
    }

    public static void virtual(Runnable task) {
        Thread.ofVirtual().start(task);
    }

    public static ThreadPoolExecutor executor(long keepAliveSeconds, BlockingQueue<Runnable> queue) {
        return new ThreadPoolExecutor(OS.cores, OS.cores, keepAliveSeconds, TimeUnit.SECONDS, queue);
    }

    public static ThreadPoolExecutor executor(long keepAliveSeconds) {
        return executor(keepAliveSeconds, new LinkedBlockingQueue<>());
    }
}
