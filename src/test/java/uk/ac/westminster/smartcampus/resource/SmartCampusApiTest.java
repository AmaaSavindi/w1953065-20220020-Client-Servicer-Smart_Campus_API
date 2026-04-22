package uk.ac.westminster.smartcampus.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.westminster.smartcampus.config.SmartCampusApplication;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;
import uk.ac.westminster.smartcampus.service.CampusStore;

class SmartCampusApiTest extends JerseyTest {

    private final CampusStore store = CampusStore.getInstance();

    @Override
    protected Application configure() {
        return ResourceConfig.forApplication(new SmartCampusApplication())
                .register(FailureResource.class);
    }

    @BeforeEach
    void setUpStore() {
        store.clear();
    }

    @Test
    void discoveryEndpointReturnsMetadataAndResourceLinks() {
        Response response = target("/").request().get();

        Map<String, Object> payload = response.readEntity(new GenericType<>() {
        });

        assertEquals(200, response.getStatus());
        assertEquals("v1", payload.get("version"));
        assertEquals("smartcampus-admin@westminster.ac.uk", payload.get("contact"));
        Map<String, String> resources = (Map<String, String>) payload.get("resources");
        assertTrue(resources.get("rooms").endsWith("/rooms"));
        assertTrue(resources.get("sensors").endsWith("/sensors"));
    }

    @Test
    void roomCreateListAndDetailFlowReturnsCreatedAndLocation() {
        Room room = new Room("LIB-301", "Library Quiet Study", 30, null);

        Response createResponse = target("rooms").request()
                .post(Entity.entity(room, MediaType.APPLICATION_JSON));
        Room createdRoom = createResponse.readEntity(Room.class);

        assertEquals(201, createResponse.getStatus());
        assertNotNull(createResponse.getLocation());
        assertTrue(createResponse.getLocation().toString().endsWith("/rooms/LIB-301"));
        assertEquals("LIB-301", createdRoom.getId());
        assertTrue(createdRoom.getSensorIds().isEmpty());

        List<Room> rooms = target("rooms").request().get(new GenericType<>() {
        });
        assertEquals(1, rooms.size());

        Room fetchedRoom = target("rooms/LIB-301").request().get(Room.class);
        assertEquals("Library Quiet Study", fetchedRoom.getName());
        assertEquals(30, fetchedRoom.getCapacity());
    }

    @Test
    void roomDeleteReturnsConflictWhenSensorsAreStillLinked() {
        store.addRoom(new Room("ENG-101", "Engineering Lab", 50, List.of()));
        store.addSensor(new Sensor("CO2-001", "CO2", "ACTIVE", 420.0, "ENG-101"));

        Response conflictResponse = target("rooms/ENG-101").request().delete();
        Map<String, Object> errorBody = conflictResponse.readEntity(new GenericType<>() {
        });

        assertEquals(409, conflictResponse.getStatus());
        assertEquals("Conflict", errorBody.get("error"));

        store.addRoom(new Room("ENG-102", "Engineering Seminar Room", 25, List.of()));
        Response deleteResponse = target("rooms/ENG-102").request().delete();
        assertEquals(204, deleteResponse.getStatus());
        assertFalse(store.roomExists("ENG-102"));
    }

    @Test
    void sensorCreateReturnsUnprocessableEntityWhenRoomDoesNotExist() {
        Sensor sensor = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "UNKNOWN-ROOM");

        Response response = target("sensors").request()
                .post(Entity.entity(sensor, MediaType.APPLICATION_JSON));
        Map<String, Object> errorBody = response.readEntity(new GenericType<>() {
        });

        assertEquals(422, response.getStatus());
        assertEquals("Unprocessable Entity", errorBody.get("error"));
    }

    @Test
    void sensorFilteringIsCaseInsensitive() {
        store.addRoom(new Room("SCI-201", "Science Lab", 40, List.of()));
        store.addSensor(new Sensor("CO2-201", "CO2", "ACTIVE", 405.0, "SCI-201"));
        store.addSensor(new Sensor("TEMP-201", "Temperature", "ACTIVE", 20.0, "SCI-201"));

        List<Sensor> filtered = target("sensors")
                .queryParam("type", "co2")
                .request()
                .get(new GenericType<>() {
                });

        assertEquals(1, filtered.size());
        assertEquals("CO2-201", filtered.get(0).getId());
    }

    @Test
    void readingsEndpointStoresHistoryAndUpdatesParentCurrentValue() {
        store.addRoom(new Room("BUS-101", "Business Lecture Hall", 120, List.of()));
        store.addSensor(new Sensor("TEMP-900", "Temperature", "ACTIVE", 19.0, "BUS-101"));

        SensorReading reading = new SensorReading(null, 1713772800000L, 24.7);

        Response postResponse = target("sensors/TEMP-900/readings").request()
                .post(Entity.entity(reading, MediaType.APPLICATION_JSON));
        SensorReading createdReading = postResponse.readEntity(SensorReading.class);

        assertEquals(201, postResponse.getStatus());
        assertNotNull(createdReading.getId());

        List<SensorReading> history = target("sensors/TEMP-900/readings").request()
                .get(new GenericType<>() {
                });
        assertEquals(1, history.size());
        assertEquals(24.7, history.get(0).getValue());
        assertEquals(24.7, store.getSensor("TEMP-900").orElseThrow().getCurrentValue());
    }

    @Test
    void maintenanceSensorsRejectNewReadings() {
        store.addRoom(new Room("MED-220", "Medical Simulation Lab", 18, List.of()));
        store.addSensor(new Sensor("OCC-777", "Occupancy", "MAINTENANCE", 0.0, "MED-220"));

        Response response = target("sensors/OCC-777/readings").request()
                .post(Entity.entity(new SensorReading(null, 1713772800000L, 1.0), MediaType.APPLICATION_JSON));
        Map<String, Object> errorBody = response.readEntity(new GenericType<>() {
        });

        assertEquals(403, response.getStatus());
        assertEquals("Forbidden", errorBody.get("error"));
    }

    @Test
    void unexpectedErrorsReturnCleanInternalServerErrorBody() {
        Response response = target("boom").request().get();
        String body = response.readEntity(String.class);

        assertEquals(500, response.getStatus());
        assertTrue(body.contains("Internal Server Error"));
        assertFalse(body.contains("RuntimeException"));
        assertFalse(body.contains("stackTrace"));
    }

    @Path("/boom")
    public static class FailureResource {

        @GET
        public Response fail() {
            throw new IllegalStateException("This should be hidden from the client.");
        }
    }
}
