package forge.util;

import arc.Core;
import arc.func.Cons3;
import arc.input.InputProcessor;
import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.struct.Seq;
import forge.ForgeLoader.AnnotationConsumer;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsCategory;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import static mindustry.game.EventType.Trigger.update;

public class SettingUtils {
    private static final Seq<SettingsCategory> categories = new Seq<>();
    
    private static final Seq<Field> changeFieldListeners = new Seq<>();
    private static final Seq<BindMethodListener> bindMethodListeners = new Seq<>();
    private static final Seq<BindFieldListener> bindFieldListeners = new Seq<>();
    
    private static final Seq<Method> invokeOnUpdateList = new Seq<>();
    
    static {
        EventUtils.on(update, t -> {
            if (Core.settings.modified()) {
                changeFieldListeners.each(field -> {
                    var key = field.getDeclaringClass().getAnnotation(Category.class).key() + '.' + field.getName();
                    var value = Core.settings.get(key, Core.settings.getDefault(key));
                    if (!ReflectUtils.get(field).equals(value)) {
                        ReflectUtils.set(field, value);
                    }
                });
            }
            invokeOnUpdateList.each(ReflectUtils::invoke);
        });
        
        Core.input.addProcessor(new InputProcessor() {
            @Override
            public boolean keyDown(KeyCode keycode) {
                for (var listener : bindMethodListeners) {
                    if (listener.bind.value.key == keycode) {
                        if (listener.method.getAnnotation(Bind.class).update()) {
                            if(!invokeOnUpdateList.remove(listener.method)) {
                                invokeOnUpdateList.add(listener.method);
                            }
                        } else {
                            ReflectUtils.invoke(listener.method);
                        }
                        return true;
                    }
                }
                for (var listener : bindFieldListeners) {
                    if (listener.bind.value.key == keycode) {
                        ReflectUtils.set(listener.field, !(boolean) ReflectUtils.get(listener.field));
                        return true;
                    }
                }
                return false;
            }
        });
    }
    
    private record BindMethodListener(Method method, KeyBind bind) { }
    private record BindFieldListener(Field field, KeyBind bind) { }
    private record SettingCell(String key, Object def, SettingsTable table) { }
    
    // btw name = key
    
    private static void register(Field field, Cons3<String, Object, SettingsTable> consumer) {
        var category = field.getDeclaringClass().getAnnotation(Category.class);
        var categoryKey = category.key();
        var key = categoryKey + '.' + field.getName();
        var def = ReflectUtils.get(field);
        
        Core.settings.defaults(key, def);
        changeFieldListeners.add(field);
        
        var settingsCategory = categories.find(c -> c.name.equals(categoryKey));
        if (settingsCategory == null) {
            var drawable = Icon.icons.get(category.icon());
            if (drawable == null) {
                drawable = Core.atlas.getDrawable(category.icon());
            }
            settingsCategory = new SettingsCategory(category.key(), drawable, t -> { });
            Vars.ui.settings.getCategories().add(settingsCategory);
            categories.add(settingsCategory);
        }
        
        ReflectUtils.set(field, Core.settings.get(key, Core.settings.getDefault(key)));
        
        consumer.get(key, def, settingsCategory.table);
    }
    
    @AnnotationConsumer
    private static void bindConsumer(Bind bind, Member obj){
        var categoryKey = obj.getDeclaringClass().getAnnotation(Category.class).key();
        var name = obj.getName();
        
        if (obj instanceof Field field) {
            var key = categoryKey + '.' + name;
            var value = Core.settings.get(key, null);
            if (value != null) {
                ReflectUtils.set(field, value);
            }
            bindFieldListeners.add(new BindFieldListener(
                field, KeyBind.add(name, KeyCode.unset, categoryKey)
            ));
        } else {
            bindMethodListeners.add(new BindMethodListener(
                (Method) obj, KeyBind.add(name, KeyCode.unset, categoryKey)
            ));
        }
    }
    
    @AnnotationConsumer
    private static void checkboxConsumer(CheckBox checkbox, Field field) {
        register(field, (key, def, table) ->
            table.checkPref(key, (boolean) def)
        );
    }
    
    @AnnotationConsumer
    private static void sliderConsumer(Slider slider, Field field) {
        register(field, (key, def, table) ->
            table.sliderPref(key, (int) def, slider.min(), slider.max(), slider.step(),
                slider.suffix().isEmpty() ? Integer::toString : i -> i + " " + Core.bundle.get(slider.suffix())
            )
        );
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Category {
        String key();
        String icon();
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface Bind {
        boolean update() default false; // for methods, subscribe for Trigger.update on bind and unsubscribe
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface CheckBox { }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Slider {
        public int min();
        public int max();
        public int step() default 1;
        public String suffix() default "";
    }
}
