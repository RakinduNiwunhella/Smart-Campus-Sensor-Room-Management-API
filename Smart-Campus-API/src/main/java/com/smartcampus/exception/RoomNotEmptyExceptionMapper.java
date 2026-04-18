package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Part 5.1 – Maps RoomNotEmptyException → HTTP 409 Conflict.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        ErrorResponse body = new ErrorResponse(
                409,
                "Conflict",
                ex.getMessage()
        );
        return Response.status(Response.Status.CONFLICT)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(body)
                       .build();
    }
}
