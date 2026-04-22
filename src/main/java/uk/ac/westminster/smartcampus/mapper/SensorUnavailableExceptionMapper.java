package uk.ac.westminster.smartcampus.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import uk.ac.westminster.smartcampus.dto.ErrorResponse;
import uk.ac.westminster.smartcampus.exception.SensorUnavailableException;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        ErrorResponse errorResponse = new ErrorResponse(403, "Forbidden", exception.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
    }
}
