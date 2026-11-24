package ru.itmo.is.lab.service;

import ru.itmo.is.lab.exception.SerializationConflictException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.RollbackException;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;

@ApplicationScoped
public class UniqueNameWrites {

    private static final int MAX_ATTEMPTS = 4;
    private static final String SERIALIZATION_FAILURE = "40001";

    @Inject
    private SerializableTransaction transaction;

    public <T> T run(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(work);
            } catch (RuntimeException | RollbackException e) {
                if (!isSerializationFailure(e)) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new IllegalStateException("Hero write transaction rolled back", e);
                }
                if (attempt == MAX_ATTEMPTS) {
                    throw new SerializationConflictException(e);
                }
            }
        }
    }

    private static boolean isSerializationFailure(Throwable failure) {
        Deque<Throwable> pending = new ArrayDeque<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(failure);
        while (!pending.isEmpty()) {
            Throwable current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (current instanceof SQLException) {
                SQLException sql = (SQLException) current;
                if (SERIALIZATION_FAILURE.equals(sql.getSQLState())) {
                    return true;
                }
                if (sql.getNextException() != null) {
                    pending.add(sql.getNextException());
                }
            }
            if (current.getCause() != null) {
                pending.add(current.getCause());
            }
            // WildFly can attach the JDBC failure as a suppressed exception at commit.
            Collections.addAll(pending, current.getSuppressed());
        }
        return false;
    }
}
