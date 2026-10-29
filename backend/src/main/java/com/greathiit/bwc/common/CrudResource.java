package com.greathiit.bwc.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通用 CRUD 资源声明：/api/crud/{value}。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrudResource {

    String value();

    String module();

    String[] search() default {};
}
