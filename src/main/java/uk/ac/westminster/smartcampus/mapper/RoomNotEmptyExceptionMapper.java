package uk.ac.westminster.smartcampus.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import uk.ac.westminster.smartcampus.dto.ErrorResponse;
import uk.ac.westminster.smartcampus.exception.RoomNotEmptyException;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        ErrorResponse errorResponse = new ErrorResponse(409, "Conflict",
                "The room cannot be deleted because it still contains active sensor links.");
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
    }
}
