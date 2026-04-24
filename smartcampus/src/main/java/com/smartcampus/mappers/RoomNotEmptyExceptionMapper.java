package com.smartcampus.mappers;

import com.smartcampus.exceptions.RoomNotEmptyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;


@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("error", "Conflict");
        body.put("message", exception.getMessage());
        body.put("hint", "Remove or reassign all sensors in this room before deleting it.");

        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
