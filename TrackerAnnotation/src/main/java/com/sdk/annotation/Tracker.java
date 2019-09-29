package com.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Tracker {
    boolean enable() default true;

    String[] group() default {};

    /**
     * construct method is enable
     *
     * @return
     */
    boolean construct() default false;

    /**
     * Customize info
     *
     * @return
     */
    String tag() default "";

    InjectType injectType() default InjectType.DEFAULT;

    InjectRule injectRule() default @InjectRule();
}
