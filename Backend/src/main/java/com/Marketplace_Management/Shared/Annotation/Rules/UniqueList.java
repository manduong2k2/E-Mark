package com.Marketplace_Management.Shared.Annotation.Rules;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueList {
    Unique[] value();
}
