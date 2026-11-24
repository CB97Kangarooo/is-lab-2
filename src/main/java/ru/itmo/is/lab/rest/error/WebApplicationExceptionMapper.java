package ru.itmo.is.lab.rest.error;

import ru.itmo.is.lab.rest.dto.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOGGER = Logger.getLogger(WebApplicationExceptionMapper.class.getName());

    @Override
    public Response toResponse(WebApplicationException e) {
        Response original = e.getResponse();
        int status = original.getStatus();
        if (status >= 500) {
            LOGGER.log(Level.SEVERE, "Ошибка JAX-RS при обработке запроса", e);
        }
        Response.ResponseBuilder builder = Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(
                        ErrorResponses.messageForStatus(status), Collections.<String, String>emptyMap()));

        String allow = original.getHeaderString(HttpHeaders.ALLOW);
        if (allow != null) {
            builder.header(HttpHeaders.ALLOW, allow);
        }
        return builder.build();
    }
}
