package com.Marketplace_Management.Shared.Annotation.Rules;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import com.Marketplace_Management.Shared.Validation.Rules.UniqueValidator;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueValidator.class)
@Repeatable(UniqueList.class)
public @interface Unique {

    boolean when() default true;

    String message() default "Value already exists";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String ignore() default "";

    String table();
    String column();
    Class<?> type() default String.class;

    String deletedAtColumn() default "";
    
    String whereClause() default "";
}
