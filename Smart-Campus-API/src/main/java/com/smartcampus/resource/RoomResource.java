package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 2 – Room Management.
 *
 * GET    /api/v1/rooms            – list all rooms
 * POST   /api/v1/rooms            – create a room
 * GET    /api/v1/rooms/{roomId}   – get one room
 * DELETE /api/v1/rooms/{roomId}   – delete a room (blocked if sensors are still assigned)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /rooms ────────────────────────────────────────────────────────────────
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    // ── POST /rooms ───────────────────────────────────────────────────────────────
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request",
                            "message", "Field 'id' is required."))
                    .build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("status", 409, "error", "Conflict",
                            "message", "Room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }
        store.getRooms().put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // ── GET /rooms/{roomId} ───────────────────────────────────────────────────────
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                            "message", "Room not found: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ── DELETE /rooms/{roomId} ────────────────────────────────────────────────────
    /**
     * Idempotency note: first call on an existing, empty room returns 204.
     * A second call (room already gone) returns 404 – the resource state is
     * the same (room absent) so the outcome is identical from the client's
     * perspective, which satisfies REST idempotency semantics.
     *
     * Safety constraint: a room that still has sensors assigned cannot be
     * deleted. RoomNotEmptyException is thrown and mapped to HTTP 409.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                            "message", "Room not found: " + roomId))
                    .build();
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }
        store.getRooms().remove(roomId);
        return Response.noContent().build(); // 204 No Content
    }
}
