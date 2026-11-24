package ru.itmo.is.lab.service;

import ru.itmo.is.lab.dto.CoordinatesRequest;
import ru.itmo.is.lab.dto.CreateCarRequest;
import ru.itmo.is.lab.dto.CreateHumanBeingRequest;
import ru.itmo.is.lab.entity.Mood;
import ru.itmo.is.lab.entity.WeaponType;
import ru.itmo.is.lab.exception.ImportInputException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ImportFileReader {

    public static final int MAX_RECORDS = 500;

    @Inject
    private Validator validator;

    public static final class Entry {
        final CreateHumanBeingRequest human;
        final CreateCarRequest car;

        Entry(CreateHumanBeingRequest human, CreateCarRequest car) {
            this.human = human;
            this.car = car;
        }
    }

    public List<Entry> read(InputStream source) {
        try (JsonReader reader = Json.createReader(source, StandardCharsets.UTF_8)) {
            JsonArray array = reader.readArray();
            if (array.isEmpty() || array.size() > MAX_RECORDS) {
                throw new ImportInputException("file", "Файл должен содержать от 1 до " + MAX_RECORDS + " записей");
            }
            List<Entry> entries = new ArrayList<>(array.size());
            for (int index = 0; index < array.size(); index++) {
                String path = "Запись " + (index + 1);
                entries.add(readEntry(object(array.get(index), path), path));
            }
            return entries;
        } catch (JsonException e) {
            throw new ImportInputException("file", "Ожидается JSON-массив в кодировке UTF-8");
        }
    }

    private Entry readEntry(JsonObject row, String path) {
        requireKnownFields(row, path, Set.of("name", "coordinates", "realHero", "hasToothpick",
                "mood", "impactSpeed", "weaponType", "car"));
        CreateHumanBeingRequest human = new CreateHumanBeingRequest();
        human.setName(string(row.get("name"), path + ".name"));
        human.setRealHero(requiredBoolean(row.get("realHero"), path + ".realHero"));
        human.setHasToothpick(requiredBoolean(row.get("hasToothpick"), path + ".hasToothpick"));
        human.setMood(enumValue(Mood.class, row.get("mood"), path + ".mood"));
        human.setWeaponType(enumValue(WeaponType.class, row.get("weaponType"), path + ".weaponType"));
        JsonNumber speed = number(row.get("impactSpeed"), path + ".impactSpeed");
        try {
            human.setImpactSpeed(speed.intValueExact());
        } catch (ArithmeticException e) {
            throw new ImportInputException(path + ".impactSpeed", "Ожидается целое число типа Integer");
        }

        String coordinatesPath = path + ".coordinates";
        JsonObject coordinates = object(row.get("coordinates"), coordinatesPath);
        requireKnownFields(coordinates, coordinatesPath, Set.of("x", "y"));
        CoordinatesRequest position = new CoordinatesRequest();
        position.setX(decimal(coordinates.get("x"), coordinatesPath + ".x"));
        position.setY(decimal(coordinates.get("y"), coordinatesPath + ".y"));
        human.setCoordinates(position);
        validate(human, path);

        if (absent(row.get("car"))) {
            return new Entry(human, null);
        }
        String carPath = path + ".car";
        JsonObject car = object(row.get("car"), carPath);
        requireKnownFields(car, carPath, Set.of("name", "cool"));
        CreateCarRequest vehicle = new CreateCarRequest();
        vehicle.setName(string(car.get("name"), carPath + ".name"));
        if (!absent(car.get("cool"))) {
            vehicle.setCool(requiredBoolean(car.get("cool"), carPath + ".cool"));
        }
        validate(vehicle, carPath);
        return new Entry(human, vehicle);
    }

    private <T> void validate(T value, String path) {
        validator.validate(value).stream()
                .min(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .ifPresent(v -> invalid(v, path));
    }

    private static void invalid(ConstraintViolation<?> violation, String path) {
        throw new ImportInputException(path + "." + violation.getPropertyPath(), violation.getMessage());
    }

    private static void requireKnownFields(JsonObject value, String path, Set<String> allowed) {
        for (String key : value.keySet()) {
            if (!allowed.contains(key)) {
                throw new ImportInputException(path + "." + key, "Неизвестное или генерируемое поле");
            }
        }
    }

    private static JsonObject object(JsonValue value, String path) {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        throw new ImportInputException(path, "Ожидается объект");
    }

    private static String string(JsonValue value, String path) {
        if (absent(value)) {
            return null;
        }
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        }
        throw new ImportInputException(path, "Ожидается строка");
    }

    private static JsonNumber number(JsonValue value, String path) {
        if (value instanceof JsonNumber) {
            return (JsonNumber) value;
        }
        throw new ImportInputException(path, "Ожидается число");
    }

    private static double decimal(JsonValue value, String path) {
        double result = number(value, path).doubleValue();
        if (!Double.isFinite(result)) {
            throw new ImportInputException(path, "Ожидается конечное число");
        }
        return result;
    }

    private static boolean requiredBoolean(JsonValue value, String path) {
        if (value == JsonValue.TRUE || value == JsonValue.FALSE) {
            return value == JsonValue.TRUE;
        }
        throw new ImportInputException(path, "Ожидается true или false");
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, JsonValue value, String path) {
        String text = string(value, path);
        if (text == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, text);
        } catch (IllegalArgumentException e) {
            throw new ImportInputException(path, "Недопустимое значение: " + text);
        }
    }

    private static boolean absent(JsonValue value) {
        return value == null || value == JsonValue.NULL;
    }
}
