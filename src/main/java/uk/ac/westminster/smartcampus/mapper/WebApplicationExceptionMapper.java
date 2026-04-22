package uk.ac.westminster.smartcampus.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import uk.ac.westminster.smartcampus.dto.ErrorResponse;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        int statusCode = exception.getResponse().getStatus();
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        String error = status != null ? status.getReasonPhrase() : "Error";
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "The request could not be processed."
                : exception.getMessage();

        ErrorResponse errorResponse = new ErrorResponse(statusCode, error, message);
        return Response.status(statusCode)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
    }
}
