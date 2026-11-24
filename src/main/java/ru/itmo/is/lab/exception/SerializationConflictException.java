package ru.itmo.is.lab.exception;

public class SerializationConflictException extends RuntimeException {

    public SerializationConflictException(Throwable cause) {
        super("Данные одновременно изменились. Повторите операцию.", cause);
    }
}
