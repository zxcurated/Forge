package forge.annotations.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** initialize class in static */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Static {
}
