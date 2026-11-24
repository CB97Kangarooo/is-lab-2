package ru.itmo.is.lab.service;

import ru.itmo.is.lab.entity.AppUser;
import ru.itmo.is.lab.entity.ImportOperation;
import ru.itmo.is.lab.entity.UserRole;
import ru.itmo.is.lab.repository.UserRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.NotAuthorizedException;
import java.util.List;

@ApplicationScoped
public class ImportHistoryService {

    public static final int PAGE_SIZE = 25;

    @PersistenceContext
    private EntityManager em;
    @Inject
    private UserRepository users;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public long start(String username) {
        ImportOperation operation = new ImportOperation(currentUser(username));
        em.persist(operation);
        return operation.getId();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void succeed(long id, int count) {
        em.find(ImportOperation.class, id).succeed(count);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void fail(long id) {
        em.find(ImportOperation.class, id).fail();
    }

    @Transactional
    public JsonObject page(String username, int offset) {
        AppUser user = currentUser(username);
        boolean admin = user.getRole() == UserRole.ADMIN;
        String filter = admin ? "" : " where u.id = :initiatorId";
        TypedQuery<ImportOperation> query = em.createQuery(
                "select o from ImportOperation o join fetch o.initiator u" + filter + " order by o.id desc",
                ImportOperation.class);
        if (!admin) {
            query.setParameter("initiatorId", user.getId());
        }
        List<ImportOperation> found = query.setFirstResult(offset).setMaxResults(PAGE_SIZE + 1).getResultList();
        JsonArrayBuilder items = Json.createArrayBuilder();
        found.stream().limit(PAGE_SIZE).map(ImportHistoryService::toJson).forEach(items::add);
        return Json.createObjectBuilder().add("items", items).add("hasMore", found.size() > PAGE_SIZE)
                .add("pageSize", PAGE_SIZE).build();
    }

    private AppUser currentUser(String username) {
        AppUser user = username == null ? null : users.findByUsername(username);
        if (user == null) {
            throw new NotAuthorizedException("Session");
        }
        return user;
    }

    private static JsonObject toJson(ImportOperation operation) {
        JsonObjectBuilder result = Json.createObjectBuilder()
                .add("id", operation.getId())
                .add("username", operation.getInitiator().getUsername())
                .add("status", operation.getStatus().name());
        if (operation.getAddedCount() != null) {
            result.add("addedCount", operation.getAddedCount());
        }
        return result.build();
    }
}
