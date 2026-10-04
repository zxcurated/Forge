package forge;

import arc.util.Log;
import arc.util.Timer;
import forge.util.SettingUtils.ChangesHandler.Bind;
import forge.util.SettingUtils.ChangesHandler.Category;
import forge.util.SettingUtils.ChangesHandler.Slider;

@Category(key = "test", icon = "add")
public class Test {
    @Slider(min = 10, max = 95, step = 5)
    private static int number = 67;
    
    @Bind
    private static boolean enabledSixSeven = false;
    
    static {
        Timer.schedule(() -> {
            Log.info(number);
            Log.info("six seven " + enabledSixSeven);
        }, 1, 2);
    }
    
    @Bind
    private static void say67() {
        Log.info(67);
    }
    
    @Bind(update = true)
    private static void sayMore67() {
        Log.info("nah i would " + 67);
    }
}
