package com.smarttrans.ai.activity;

import com.smarttrans.ai.activity.ActivityService.ActivityResponse;
import com.smarttrans.ai.security.Access;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/activities")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ActivityResource {

    @Inject
    private ActivityService activities;

    @Inject
    private Access access;

    @GET
    @Path("/{id}")
    public ActivityResponse getById(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return activities.getById(id);
    }

    @GET
    public List<ActivityResponse> getAll() {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return activities.getAll();
    }
}
