package com.smarttrans.ai.mission;

import com.smarttrans.ai.mission.MissionService.CreateMissionRequest;
import com.smarttrans.ai.mission.MissionService.MissionResponse;
import com.smarttrans.ai.mission.MissionService.StatusRequest;
import com.smarttrans.ai.mission.MissionService.UpdateMissionRequest;
import com.smarttrans.ai.security.Access;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/missions")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class MissionResource {

    @Inject
    private MissionService missions;

    @Inject
    private Access access;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateMissionRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return Response.status(Response.Status.CREATED).entity(missions.create(request)).build();
    }

    @GET
    @Path("/{id}")
    public MissionResponse getById(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER", "DRIVER");
        return missions.getById(id);
    }

    @GET
    public List<MissionResponse> getAll() {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER", "DRIVER");
        return missions.getAll();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public MissionResponse update(@PathParam("id") UUID id, @Valid UpdateMissionRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return missions.update(id, request);
    }

    @PATCH
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public MissionResponse updateStatus(@PathParam("id") UUID id, @Valid StatusRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER", "DRIVER");
        return missions.updateStatus(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        missions.delete(id);
        return Response.noContent().build();
    }
}
