package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Part 5.3 – Maps SensorUnavailableException → HTTP 403 Forbidden.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        ErrorResponse body = new ErrorResponse(
                403,
                "Forbidden",
                ex.getMessage()
        );
        return Response.status(Response.Status.FORBIDDEN)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(body)
                       .build();
    }
}
