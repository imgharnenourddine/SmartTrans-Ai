package com.smarttrans.ai.health;

import com.smarttrans.ai.bootstrap.DatabaseProbe;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Same path the React dev proxy and frontend Nginx already call.
 * MicroProfile Health publishes {@code /health}, which those configs do not use.
 */
@Path("/actuator/health")
@RequestScoped
public class ActuatorHealthResource {

    private static final Logger LOG = Logger.getLogger(ActuatorHealthResource.class.getName());

    @Inject
    private DatabaseProbe databaseProbe;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        try {
            long roles = databaseProbe.countRoles();
            String body = """
                    {"status":"UP","components":{"db":{"status":"UP","roles":%d}}}
                    """.formatted(roles).strip();
            return Response.ok(body, MediaType.APPLICATION_JSON).build();
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Database health check failed", ex);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"status\":\"DOWN\",\"components\":{\"db\":{\"status\":\"DOWN\"}}}")
                    .build();
        }
    }
}
