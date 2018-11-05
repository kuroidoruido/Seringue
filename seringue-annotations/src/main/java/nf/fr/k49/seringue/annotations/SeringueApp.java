package nf.fr.k49.seringue.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(SOURCE)
@Target(TYPE)
/**
 * <p>A SingletonApp annotation must be placed on the main class of the application.
 * 
 * <p>Notice you should use one and only once the SingletonApp annotation.
 * 
 * @author Anthony Pena <anthony.pena_at_outlook.fr>
 *
 */
public @interface SeringueApp {

}
