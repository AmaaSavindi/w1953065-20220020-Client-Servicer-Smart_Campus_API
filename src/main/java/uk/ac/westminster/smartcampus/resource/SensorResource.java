package uk.ac.westminster.smartcampus.resource;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import uk.ac.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.service.CampusStore;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final CampusStore store = CampusStore.getInstance();

    @POST
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        validateSensor(sensor);

        if (!store.roomExists(sensor.getRoomId().trim())) {
            throw new LinkedResourceNotFoundException("The requested roomId does not exist.");
        }

        Sensor sensorToStore = new Sensor();
        sensorToStore.setId(sensor.getId().trim());
        sensorToStore.setType(sensor.getType().trim());
        sensorToStore.setStatus(sensor.getStatus().trim());
        sensorToStore.setCurrentValue(sensor.getCurrentValue());
        sensorToStore.setRoomId(sensor.getRoomId().trim());

        if (!store.addSensor(sensorToStore)) {
            throw new WebApplicationException("Sensor already exists.", Response.Status.CONFLICT);
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(sensorToStore.getId()).build();
        return Response.created(location).entity(store.getSensor(sensorToStore.getId()).orElse(sensorToStore)).build();
    }

    @GET
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = store.getAllSensors();
        if (type == null || type.isBlank()) {
            return sensors;
        }

        String expectedType = type.trim();
        return sensors.stream()
                .filter(sensor -> sensor.getType() != null && sensor.getType().equalsIgnoreCase(expectedType))
                .collect(Collectors.toList());
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadings(@PathParam("sensorId") String sensorId) {
        Optional<Sensor> sensor = store.getSensor(sensorId);
        if (sensor.isEmpty()) {
            throw new NotFoundException("Sensor not found.");
        }
        return new SensorReadingResource(sensorId);
    }

    private void validateSensor(Sensor sensor) {
        if (sensor == null) {
            throw new BadRequestException("Sensor payload is required.");
        }
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            throw new BadRequestException("Sensor id is required.");
        }
        if (sensor.getType() == null || sensor.getType().isBlank()) {
            throw new BadRequestException("Sensor type is required.");
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            throw new BadRequestException("Sensor status is required.");
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            throw new BadRequestException("Sensor roomId is required.");
        }
    }
}
