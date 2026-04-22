package uk.ac.westminster.smartcampus.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import uk.ac.westminster.smartcampus.dto.ErrorResponse;
import uk.ac.westminster.smartcampus.exception.LinkedResourceNotFoundException;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        ErrorResponse errorResponse = new ErrorResponse(422, "Unprocessable Entity", exception.getMessage());
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
    }
}
