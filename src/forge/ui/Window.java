package forge.ui;

import arc.scene.ui.layout.WidgetGroup;

import static mindustry.ui.Styles.black8;

public class Window extends WidgetGroup {
    
    @Override
    public void draw() {
        black8.draw(x, y, width, height);
        super.draw();
    }
}
