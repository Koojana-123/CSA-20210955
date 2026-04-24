package com.smartcampus.resources;

import com.smartcampus.exceptions.SensorUnavailableException;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return errorResponse(Response.Status.NOT_FOUND,
                    "Sensor '" + sensorId + "' not found.");
        }
        List<SensorReading> history = store.getReadings(sensorId);
        return Response.ok(history).build();
    }

    
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return errorResponse(Response.Status.NOT_FOUND,
                    "Sensor '" + sensorId + "' not found.");
        }

       
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE " +
                    "and cannot accept new readings.");
        }

        
        if (reading.getId() == null || reading.getId().isBlank()) {
            SensorReading fresh = new SensorReading(reading.getValue());
            reading.setId(fresh.getId());
            reading.setTimestamp(fresh.getTimestamp());
        }

        store.addReading(sensorId, reading);

        
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

   
    private Response errorResponse(Response.Status status, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return Response.status(status).entity(body).build();
    }
}
