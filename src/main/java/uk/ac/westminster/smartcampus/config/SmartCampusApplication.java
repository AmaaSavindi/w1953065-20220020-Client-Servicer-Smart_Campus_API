package uk.ac.westminster.smartcampus.config;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import uk.ac.westminster.smartcampus.resource.DiscoveryResource;
import uk.ac.westminster.smartcampus.resource.RoomResource;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        return classes;
    }
}
