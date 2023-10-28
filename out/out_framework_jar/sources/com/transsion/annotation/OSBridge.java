package com.transsion.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE})
@Repeatable(Container.class)
@Retention(RetentionPolicy.RUNTIME)
/* loaded from: classes4.dex */
public @interface OSBridge {

    /* loaded from: classes4.dex */
    public enum Client {
        LICE_INTERFACE_FRAMEWORK,
        LICE_INTERFACE_SERVICES,
        LICE_CORE_FRAMEWORK,
        LICE_CORE_SERVICES,
        OTHER
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    /* loaded from: classes4.dex */
    public @interface Container {
        OSBridge[] value();
    }

    Client client() default Client.OTHER;
}
