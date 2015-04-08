package kfs.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author pavedrim
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)

public @interface KfsSearchCriteria {
    boolean like() default false;
    String name() default "";
    String operator() default "=";
    String caption() default "";
    int pos() default 0;
}
