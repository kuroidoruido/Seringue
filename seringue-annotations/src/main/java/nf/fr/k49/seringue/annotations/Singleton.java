package nf.fr.k49.seringue.annotations;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target({ CONSTRUCTOR })
@Documented
/**
 * A Singleton is a bean that will be instantiate only once by Seringue.
 * 
 * @author Anthony Pena <anthony.pena_at_outlook.fr>
 *
 */
public @interface Singleton {
}
