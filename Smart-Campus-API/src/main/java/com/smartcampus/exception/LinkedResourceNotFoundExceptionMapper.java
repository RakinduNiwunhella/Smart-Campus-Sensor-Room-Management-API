package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Part 5.2 – Maps LinkedResourceNotFoundException → HTTP 422 Unprocessable Entity.
 *
 * 422 is more semantically accurate than 404 here because the request URL itself is valid
 * (/api/v1/sensors), but the JSON payload references a resource (roomId) that does not exist.
 * The server understood the request but cannot process it due to a semantic error in the body.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                422,
                "Unprocessable Entity",
                ex.getMessage()
        );
        return Response.status(422)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(body)
                       .build();
    }
}
