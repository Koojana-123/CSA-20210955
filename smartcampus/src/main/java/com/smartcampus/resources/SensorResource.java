package com.smartcampus.resources;

import com.smartcampus.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 3 – Sensor Operations
 * Base path: /api/v1/sensors
 *
 * Also acts as the parent for the sub-resource locator pattern (Part 4).
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /api/v1/sensors  (optional ?type=CO2) ────────────────────────────
    /**
     * Returns all sensors, optionally filtered by type via query parameter.
     *
     * Using @QueryParam for filtering is preferable to a path segment
     * (e.g. /sensors/type/CO2) because query parameters are semantically
     * optional — they narrow a collection without implying a hierarchical
     * resource identity. Path parameters suggest a new resource level.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();

        if (type != null && !type.isBlank()) {
            List<Sensor> filtered = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }

        return Response.ok(all).build();
    }

    // ── POST /api/v1/sensors ─────────────────────────────────────────────────
    /**
     * Registers a new sensor.
     *
     * Validates that the referenced roomId actually exists. If not, throws
     * LinkedResourceNotFoundException → 422 Unprocessable Entity.
     *
     * @Consumes(APPLICATION_JSON) means JAX-RS will reject any request whose
     * Content-Type is not application/json with a 415 Unsupported Media Type.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return errorResponse(Response.Status.BAD_REQUEST, "Sensor id is required.");
        }
        if (store.sensorExists(sensor.getId())) {
            return errorResponse(Response.Status.CONFLICT,
                    "A sensor with id '" + sensor.getId() + "' already exists.");
        }

        // Referential integrity: roomId must exist
        if (sensor.getRoomId() == null || !store.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room '" + sensor.getRoomId() + "' does not exist. " +
                    "Please create the room before registering sensors in it.");
        }

        // Default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.putSensor(sensor);

        // Link sensor to room
        Room room = store.getRoom(sensor.getRoomId());
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // ── GET /api/v1/sensors/{sensorId} ───────────────────────────────────────
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return errorResponse(Response.Status.NOT_FOUND,
                    "Sensor '" + sensorId + "' not found.");
        }
        return Response.ok(sensor).build();
    }

    // ── DELETE /api/v1/sensors/{sensorId} ────────────────────────────────────
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return errorResponse(Response.Status.NOT_FOUND,
                    "Sensor '" + sensorId + "' not found.");
        }

        // Unlink from room
        Room room = store.getRoom(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }

        store.deleteSensor(sensorId);
        return Response.noContent().build();
    }

    // ── PUT /api/v1/sensors/{sensorId} ───────────────────────────────────────
    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updated) {
        Sensor existing = store.getSensor(sensorId);
        if (existing == null) {
            return errorResponse(Response.Status.NOT_FOUND,
                    "Sensor '" + sensorId + "' not found.");
        }
        if (updated.getType() != null)   existing.setType(updated.getType());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        existing.setCurrentValue(updated.getCurrentValue());
        return Response.ok(existing).build();
    }

    // ── Sub-Resource Locator: /api/v1/sensors/{sensorId}/readings ───────────
    /**
     * Part 4 – Sub-Resource Locator Pattern.
     *
     * Rather than mapping every nested path in this class, we delegate to a
     * dedicated SensorReadingResource. JAX-RS calls this method first, receives
     * the resource instance, and then continues matching the remaining path
     * against that resource's own @Path annotations.
     *
     * Benefits: single-responsibility, reduced complexity, independently
     * testable, and cleanly separates reading history from sensor management.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private Response errorResponse(Response.Status status, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return Response.status(status).entity(body).build();
    }
}
