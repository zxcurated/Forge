package forge.annotations.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Initialize class in real static
 * @see Initializer */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Static {
}