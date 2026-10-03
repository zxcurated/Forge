package forge.ui;

import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.Touchable;
import mindustry.Vars;

public class Hud extends Group{
    private static final Hud hud = new Hud();

    static {
        Vars.ui.hudGroup.addChildAfter(Vars.ui.hudGroup.find("minimap/position"), hud);
    }
    
    private Hud(){
        name = "forge-hud";
        fillParent = true;
        touchable = Touchable.childrenOnly;
    }

    public static void add(Element... elements) {
        for (Element element : elements) {
            add(element);
        }
    }

    public static void add(Element element) {
        hud.addChild(element);
    }
}
