package ru.itmo.is.lab.rest;

import ru.itmo.is.lab.exception.BadRequestException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

@ApplicationScoped
public class RequestValidator {

    @Inject
    private Validator validator;

    public <T> T require(T body) {
        if (body == null) {
            throw new BadRequestException("Тело запроса обязательно");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(body);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return body;
    }
}
