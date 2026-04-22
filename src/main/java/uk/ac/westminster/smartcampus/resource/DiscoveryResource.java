package uk.ac.westminster.smartcampus.resource;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Map<String, Object> getApiRoot(@Context UriInfo uriInfo) {
        String baseUri = uriInfo.getBaseUri().toString();

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", baseUri + "rooms");
        resources.put("sensors", baseUri + "sensors");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("version", "v1");
        response.put("contact", "smartcampus-admin@westminster.ac.uk");
        response.put("resources", resources);
        return response;
    }
}
