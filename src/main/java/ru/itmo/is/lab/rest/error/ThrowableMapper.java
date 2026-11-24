package ru.itmo.is.lab.rest.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ThrowableMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(ThrowableMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof Error) {
            LOGGER.log(Level.SEVERE, "Error при обработке запроса, ответ отдаёт контейнер", e);
            throw (Error) e;
        }
        return ErrorResponses.toResponse(ErrorResponses.resolve(e));
    }
}
