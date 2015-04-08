package kfs.csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author pavedrim
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Csv {
    String inner() default "";
    String sorting() default "";
    String name() default "";
    String sqlname() default "";
    Class<? extends CsvStrConvertor> conv() default CsvStrConvertor.class;
}
