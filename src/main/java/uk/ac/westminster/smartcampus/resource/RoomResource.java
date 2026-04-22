package uk.ac.westminster.smartcampus.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import uk.ac.westminster.smartcampus.exception.RoomNotEmptyException;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.service.CampusStore;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final CampusStore store = CampusStore.getInstance();

    @GET
    public List<Room> getRooms() {
        return store.getAllRooms();
    }

    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        validateRoom(room);

        Room roomToStore = new Room();
        roomToStore.setId(room.getId().trim());
        roomToStore.setName(room.getName().trim());
        roomToStore.setCapacity(room.getCapacity());
        roomToStore.setSensorIds(new ArrayList<>());

        if (!store.addRoom(roomToStore)) {
            throw new WebApplicationException("Room already exists.", Response.Status.CONFLICT);
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(roomToStore.getId()).build();
        return Response.created(location).entity(store.getRoom(roomToStore.getId()).orElse(roomToStore)).build();
    }

    @GET
    @Path("/{roomId}")
    public Room getRoom(@PathParam("roomId") String roomId) {
        Optional<Room> room = store.getRoom(roomId);
        return room.orElseThrow(() -> new NotFoundException("Room not found."));
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        if (!store.roomExists(roomId)) {
            throw new NotFoundException("Room not found.");
        }

        if (store.roomHasSensors(roomId)) {
            throw new RoomNotEmptyException("Room still has linked sensors.");
        }

        store.deleteRoom(roomId);
        return Response.noContent().build();
    }

    private void validateRoom(Room room) {
        if (room == null) {
            throw new BadRequestException("Room payload is required.");
        }
        if (room.getId() == null || room.getId().isBlank()) {
            throw new BadRequestException("Room id is required.");
        }
        if (room.getName() == null || room.getName().isBlank()) {
            throw new BadRequestException("Room name is required.");
        }
        if (room.getCapacity() < 0) {
            throw new BadRequestException("Room capacity must be zero or greater.");
        }
    }
}
