package com.smartcampus.resources;

import com.smartcampus.exceptions.RoomNotEmptyException;
import com.smartcampus.models.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 2 – Room Management
 * Base path: /api/v1/rooms
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /api/v1/rooms ────────────────────────────────────────────────────
    /**
     * Returns the full list of all rooms currently in the system.
     * Returning full objects avoids a second round-trip that would be needed
     * if only IDs were returned; this trades some bandwidth for fewer HTTP calls.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(rooms).build();
    }

    // ── POST /api/v1/rooms ───────────────────────────────────────────────────
    /**
     * Creates a new room. The client must supply id, name, and capacity.
     * Returns 201 Created with the persisted room object.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room id is required."))
                    .build();
        }
        if (store.roomExists(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A room with id '" + room.getId() + "' already exists."))
                    .build();
        }
        store.putRoom(room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // ── GET /api/v1/rooms/{roomId} ───────────────────────────────────────────
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ── DELETE /api/v1/rooms/{roomId} ────────────────────────────────────────
    /**
     * Deletes a room only if it has no sensors assigned.
     * This operation is idempotent: the first call deletes the room (204),
     * subsequent calls return 404 — neither causes a harmful side-effect,
     * satisfying the REST idempotency contract.
     *
     * Safety: throws RoomNotEmptyException (→ 409) if sensors are still linked.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }

        // Business logic: block deletion if sensors are still assigned
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Cannot delete room '" + roomId + "'. It still has " +
                    room.getSensorIds().size() + " sensor(s) assigned: " +
                    room.getSensorIds()
            );
        }

        store.deleteRoom(roomId);
        return Response.noContent().build(); // 204
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}
