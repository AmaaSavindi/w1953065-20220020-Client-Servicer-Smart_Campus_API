package uk.ac.westminster.smartcampus.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import uk.ac.westminster.smartcampus.dto.ErrorResponse;

@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            int status = webApplicationException.getResponse().getStatus();
            Response.Status responseStatus = Response.Status.fromStatusCode(status);
            String reason = responseStatus != null ? responseStatus.getReasonPhrase() : "Error";
            ErrorResponse errorResponse = new ErrorResponse(status, reason, webApplicationException.getMessage());
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(errorResponse)
                    .build();
        }

        ErrorResponse errorResponse = new ErrorResponse(500, "Internal Server Error",
                "An unexpected error occurred while processing the request.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
    }
}
