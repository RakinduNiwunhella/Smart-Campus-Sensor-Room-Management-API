package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 – Sensor Operations.
 *
 * GET  /api/v1/sensors – list all sensors (optional ?type= filter)
 * POST /api/v1/sensors – register a new sensor (validates roomId)
 * GET  /api/v1/sensors/{sensorId} – get one sensor
 *
 * Part 4 – Sub-Resource Locator
 * ANY  /api/v1/sensors/{sensorId}/readings  → delegates to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // GET /sensors[?type=xxx] 
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> list = new ArrayList<>(store.getSensors().values());
        if (type != null && !type.trim().isEmpty()) {
            list = list.stream()
                       .filter(s -> s.getType().equalsIgnoreCase(type.trim()))
                       .collect(Collectors.toList());
        }
        return Response.ok(list).build();
    }

    //  POST /sensors 
    /**
     * Registers a new sensor.
     * Business rule: the roomId in the request body must reference an existing room.
     * If not, LinkedResourceNotFoundException is thrown → mapped to HTTP 422.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request",
                            "message", "Field 'id' is required."))
                    .build();
        }
        // Validate that the referenced room exists
        if (sensor.getRoomId() == null || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room '" + sensor.getRoomId() + "' does not exist. " +
                    "Create the room before registering a sensor in it.");
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("status", 409, "error", "Conflict",
                            "message", "Sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }
        // Default status to ACTIVE if not supplied
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }
        store.getSensors().put(sensor.getId(), sensor);
        store.getSensorReadings().put(sensor.getId(), new ArrayList<>());
        // Keep the room's sensorIds list consistent
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // GET /sensors/{sensorId}
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                            "message", "Sensor not found: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // Sub-Resource Locator: /sensors/{sensorId}/readings
    /**
     * No HTTP-method annotation → this is a sub-resource locator.
     * JAX-RS delegates all requests under {sensorId}/readings to the
     * returned SensorReadingResource instance, passing the sensor context.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .type(MediaType.APPLICATION_JSON)
                            .entity(Map.of("status", 404, "error", "Not Found",
                                    "message", "Sensor not found: " + sensorId))
                            .build());
        }
        return new SensorReadingResource(sensorId);
    }
}
