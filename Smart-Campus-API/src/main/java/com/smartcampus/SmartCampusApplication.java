package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS Application entry point.
 
 Extends Jersey's ResourceConfig (which itself extends Application) and uses
 the packages() method to scan all classes in com.smartcampus and its
 sub-packages – picking up @Path resources, @Provider exception mappers,
 and @Provider filters automatically.
 
  @ApplicationPath sets the versioned base URL for the entire API.
  With Servlet 3.0+ (Tomcat 7+) this class is discovered automatically
  without any servlet registration in web.xml.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Scan for all resources, providers (mappers) and filters in this package
        packages("com.smartcampus");
    }
}
