package uk.ac.westminster.smartcampus.mapper;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import uk.ac.westminster.smartcampus.dto.ErrorResponse;

@Provider
public class NotSupportedExceptionMapper implements ExceptionMapper<NotSupportedException> {

    @Override
    public Response toResponse(NotSupportedException exception) {
        ErrorResponse errorResponse = new ErrorResponse(415, "Unsupported Media Type",
                "This endpoint only accepts application/json.");
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
    }
}
