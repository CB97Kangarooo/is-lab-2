package ru.itmo.is.lab.service;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.RollbackException;
import javax.transaction.Transactional;
import java.util.function.Supplier;

@ApplicationScoped
public class SerializableTransaction {

    @PersistenceContext
    private EntityManager em;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public <T> T execute(Supplier<T> work) throws RollbackException {
        // Set the isolation of this transaction before it reads or writes application data.
        em.createNativeQuery("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE").executeUpdate();
        return work.get();
    }
}
