package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 – Global "safety net" mapper.
 *
 * Catches any Throwable not handled by a more specific mapper.
 * Returns HTTP 500 with a generic JSON body – the raw stack trace is
 * intentionally withheld from the response to prevent information leakage
 * (e.g., class names, library versions, internal paths that attackers could exploit).
 * The full trace is still written to the server log for developers.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // Log the full stack trace server-side only
        LOGGER.log(Level.SEVERE, "Unhandled exception intercepted by GlobalExceptionMapper", ex);

        ErrorResponse body = new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please contact the system administrator."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(body)
                       .build();
    }
}
