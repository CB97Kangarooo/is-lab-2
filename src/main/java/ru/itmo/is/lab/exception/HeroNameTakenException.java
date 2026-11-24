package ru.itmo.is.lab.exception;

public class HeroNameTakenException extends RuntimeException {

    private final String field;

    public HeroNameTakenException(String name, Integer record) {
        super((record == null ? "" : "Запись " + record + ": ") + "Имя героя «" + name + "» уже занято");
        field = record == null ? "name" : "Запись " + record + ".name";
    }

    public String getField() {
        return field;
    }
}
