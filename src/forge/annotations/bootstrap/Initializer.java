package forge.annotations.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Methods like initialize blocks, but for {@link forge.annotations.bootstrap.Static @Static} classes
 * @see Static */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Initializer {

}