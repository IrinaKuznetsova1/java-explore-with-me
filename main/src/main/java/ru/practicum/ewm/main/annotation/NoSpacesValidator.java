package ru.practicum.ewm.main.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoSpacesValidator implements ConstraintValidator<NoSpaces, String> {
    @Override
    public boolean isValid(String str, ConstraintValidatorContext context) {
        if (str == null) return true;
        return !str.contains(" ")
                && !str.contains("\n")
                && !str.contains("\t");
    }
}
