package com.smartcampus.mappers;

import com.smartcampus.exceptions.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;


@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 422);
        body.put("error", "Unprocessable Entity");
        body.put("message", exception.getMessage());
        body.put("hint", "Ensure the roomId in your request body refers to an existing room.");

        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
