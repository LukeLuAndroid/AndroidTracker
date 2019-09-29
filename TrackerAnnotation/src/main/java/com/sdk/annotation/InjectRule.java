package com.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRule {
    /**
     * the regex string
     *
     * @return
     */
    String regex() default "";

    /**
     * the modifier array
     *
     * @return
     */
    Modifier[] attrs() default {};

    /**
     * is main rule
     * @return
     */
    boolean isMainRule() default false;
}
