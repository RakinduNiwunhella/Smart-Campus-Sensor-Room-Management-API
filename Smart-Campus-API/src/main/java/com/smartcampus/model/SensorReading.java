package com.smartcampus.model;

import java.util.UUID;

/**
 * Represents a single timestamped reading captured by a sensor.
 */
public class SensorReading {

    private String id;        // UUID of this reading event
    private long timestamp;   // epoch milliseconds when the reading was captured
    private double value;     // the actual metric value recorded

    public SensorReading() {}

    public SensorReading(double value) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value = value;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
