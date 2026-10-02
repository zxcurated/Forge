package forge.ui;

import arc.Core;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import mindustry.Vars;

public class Hud {
    private static final Group hudGroup = new Group() {
        {
            this.fillParent = true;
            this.touchable = Touchable.childrenOnly;
        }
    };

    static {
        String minimapName = Core.bundle.get("minimap");
        Vars.ui.hudGroup.addChildBefore(
            Vars.ui.hudGroup.find(
                    element -> ((Label) ((Group) element).getChildren().get(0)).getText().equals(minimapName)),
            hudGroup);
    }

    public static void add(Element... elements) {
        for (Element element : elements) {
            add(element);
        }
    }

    public static void add(Element element) {
        hudGroup.addChild(element);
    }
}
