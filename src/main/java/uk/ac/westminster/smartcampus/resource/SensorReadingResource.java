package uk.ac.westminster.smartcampus.resource;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;
import uk.ac.westminster.smartcampus.service.CampusStore;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final CampusStore store = CampusStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> getReadings() {
        ensureSensorExists();
        return store.getReadings(sensorId);
    }

    @POST
    public Response createReading(SensorReading reading, @Context UriInfo uriInfo) {
        validateReading(reading);

        Sensor sensor = ensureSensorExists();
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new ForbiddenException("Sensor is currently unavailable for new readings.");
        }

        String readingId = reading.getId() == null || reading.getId().isBlank()
                ? UUID.randomUUID().toString()
                : reading.getId().trim();

        if (store.readingExists(sensorId, readingId)) {
            throw new WebApplicationException("Reading already exists.", Response.Status.CONFLICT);
        }

        SensorReading readingToStore = new SensorReading();
        readingToStore.setId(readingId);
        readingToStore.setTimestamp(reading.getTimestamp() > 0 ? reading.getTimestamp() : System.currentTimeMillis());
        readingToStore.setValue(reading.getValue());

        store.addReading(sensorId, readingToStore);

        URI location = uriInfo.getAbsolutePathBuilder().path(readingToStore.getId()).build();
        return Response.created(location).entity(readingToStore).build();
    }

    private Sensor ensureSensorExists() {
        Optional<Sensor> sensor = store.getSensor(sensorId);
        return sensor.orElseThrow(() -> new NotFoundException("Sensor not found."));
    }

    private void validateReading(SensorReading reading) {
        if (reading == null) {
            throw new BadRequestException("Reading payload is required.");
        }
    }
}
