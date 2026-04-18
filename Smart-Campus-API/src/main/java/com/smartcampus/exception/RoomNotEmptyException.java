package com.smartcampus.exception;

/**
 * Part 5.1 – Thrown when a DELETE is attempted on a room that still has sensors.
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' cannot be deleted because it still has active sensors assigned to it. "
              + "Remove or reassign all sensors before decommissioning the room.");
        this.roomId = roomId;
    }

    public String getRoomId() { return roomId; }
}
