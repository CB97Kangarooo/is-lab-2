package ru.itmo.is.lab.exception;

public class ImportInputException extends RuntimeException {

    private final String field;

    public ImportInputException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
