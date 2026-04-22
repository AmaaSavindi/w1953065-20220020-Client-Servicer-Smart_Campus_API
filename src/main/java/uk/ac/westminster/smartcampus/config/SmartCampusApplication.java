package uk.ac.westminster.smartcampus.config;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import uk.ac.westminster.smartcampus.filter.ApiLoggingFilter;
import uk.ac.westminster.smartcampus.mapper.BadRequestExceptionMapper;
import uk.ac.westminster.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import uk.ac.westminster.smartcampus.mapper.NotFoundExceptionMapper;
import uk.ac.westminster.smartcampus.mapper.NotSupportedExceptionMapper;
import uk.ac.westminster.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import uk.ac.westminster.smartcampus.mapper.SensorUnavailableExceptionMapper;
import uk.ac.westminster.smartcampus.mapper.ThrowableExceptionMapper;
import uk.ac.westminster.smartcampus.mapper.WebApplicationExceptionMapper;
import uk.ac.westminster.smartcampus.resource.DiscoveryResource;
import uk.ac.westminster.smartcampus.resource.RoomResource;
import uk.ac.westminster.smartcampus.resource.SensorResource;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);
        classes.add(ApiLoggingFilter.class);
        classes.add(BadRequestExceptionMapper.class);
        classes.add(NotFoundExceptionMapper.class);
        classes.add(NotSupportedExceptionMapper.class);
        classes.add(WebApplicationExceptionMapper.class);
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(ThrowableExceptionMapper.class);
        return classes;
    }
}
