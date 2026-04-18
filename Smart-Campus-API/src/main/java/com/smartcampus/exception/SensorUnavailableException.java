package com.smartcampus.exception;

/**
 * Part 5.3 – Thrown when a POST reading is attempted on a sensor in MAINTENANCE or OFFLINE status.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String sensorId) {
        super("Sensor '" + sensorId + "' is currently unavailable (MAINTENANCE or OFFLINE) "
              + "and cannot accept new readings.");
    }
}
