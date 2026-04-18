package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 – API Observability Filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter so a single
 * class handles all cross-cutting logging concerns without touching individual
 * resource methods.
 *
 * @Provider makes Jersey pick this up during package scanning.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Invoked before the request is dispatched to a resource method.
     * Logs HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("--> [%s] %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /**
     * Invoked after the resource method returns, before the response is written.
     * Logs HTTP status code so operators can spot 4xx/5xx trends in logs.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("<-- [%d] %s %s",
                responseContext.getStatus(),
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }
}
