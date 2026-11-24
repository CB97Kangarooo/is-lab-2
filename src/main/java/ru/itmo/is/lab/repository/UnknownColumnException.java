package ru.itmo.is.lab.repository;

import ru.itmo.is.lab.exception.BadRequestException;

public class UnknownColumnException extends BadRequestException {
    public UnknownColumnException(String message) {
        super(message);
    }
}
