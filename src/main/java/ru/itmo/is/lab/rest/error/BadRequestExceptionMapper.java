package ru.itmo.is.lab.rest.error;

import ru.itmo.is.lab.exception.BadRequestException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {

    @Override
    public Response toResponse(BadRequestException e) {
        return ErrorResponses.toResponse(ErrorResponses.badRequest(e.getMessage()));
    }
}
