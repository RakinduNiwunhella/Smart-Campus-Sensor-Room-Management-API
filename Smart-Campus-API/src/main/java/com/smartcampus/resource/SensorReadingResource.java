package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Part 4 – Sub-Resource for sensor reading history.
 *
 * This class is not registered directly with JAX-RS; it is returned by the
 * sub-resource locator in SensorResource. The sensorId context is injected
 * via the constructor.
 *
 * GET  /api/v1/sensors/{sensorId}/readings   – retrieve full reading history
 * POST /api/v1/sensors/{sensorId}/readings   – append a new reading
 *      Side-effect: updates currentValue on the parent Sensor object.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ── GET /readings ─────────────────────────────────────────────────────────────
    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getSensorReadings()
                .getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    // ── POST /readings ────────────────────────────────────────────────────────────
    /**
     * Appends a new reading for this sensor.
     *
     * State constraint: sensors in MAINTENANCE or OFFLINE status cannot receive
     * new readings → SensorUnavailableException → HTTP 403 Forbidden.
     *
     * Side-effect: the sensor's currentValue is updated to reflect the new reading.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        // Guard: reject readings for unavailable sensors
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())
                || "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        // Auto-generate id and timestamp if the client omitted them
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.getSensorReadings()
             .computeIfAbsent(sensorId, k -> new ArrayList<>())
             .add(reading);

        // Side-effect: keep the sensor's currentValue in sync
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
