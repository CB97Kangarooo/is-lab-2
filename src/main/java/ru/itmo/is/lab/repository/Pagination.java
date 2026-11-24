package ru.itmo.is.lab.repository;

import ru.itmo.is.lab.exception.BadRequestException;

public final class Pagination {

    public static final int MAX_LIMIT = 100;

    private Pagination() {
    }

    public static void validate(int offset, int limit) {
        if (offset < 0) {
            throw new BadRequestException("offset должен быть >= 0");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException("limit должен быть от 1 до " + MAX_LIMIT);
        }
    }
}
