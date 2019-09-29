package com.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Injector {
    String value() default "default";

    InjectType type() default InjectType.DEFAULT;
}
