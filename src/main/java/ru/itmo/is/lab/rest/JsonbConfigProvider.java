package ru.itmo.is.lab.rest;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class JsonbConfigProvider implements ContextResolver<Jsonb> {

    private static final Logger LOGGER = Logger.getLogger(JsonbConfigProvider.class.getName());

    private final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withNullValues(true));

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }

    @PreDestroy
    void close() {
        try {
            jsonb.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Не удалось закрыть Jsonb", e);
        }
    }
}
