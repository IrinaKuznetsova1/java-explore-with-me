package ru.practicum.ewm.main.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoSpacesValidator.class)
@Documented
public @interface NoSpaces {

    String message() default "Invalid login";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
