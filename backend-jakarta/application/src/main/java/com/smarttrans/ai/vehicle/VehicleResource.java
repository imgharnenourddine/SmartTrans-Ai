package com.smarttrans.ai.vehicle;

import com.smarttrans.ai.domain.VehicleStatus;
import com.smarttrans.ai.security.Access;
import com.smarttrans.ai.vehicle.VehicleService.CreateVehicleRequest;
import com.smarttrans.ai.vehicle.VehicleService.UpdateVehicleRequest;
import com.smarttrans.ai.vehicle.VehicleService.VehicleResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/vehicles")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class VehicleResource {

    @Inject
    private VehicleService vehicles;

    @Inject
    private Access access;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateVehicleRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return Response.status(Response.Status.CREATED).entity(vehicles.create(request)).build();
    }

    @GET
    @Path("/expiring-documents")
    public List<VehicleResponse> expiringDocuments(@QueryParam("withinDays") @DefaultValue("30") int withinDays) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return vehicles.findWithExpiringDocuments(withinDays);
    }

    @GET
    public List<VehicleResponse> search(
            @QueryParam("search") String search,
            @QueryParam("status") VehicleStatus status,
            @QueryParam("availableOnly") Boolean availableOnly,
            @QueryParam("insuranceExpiringWithinDays") Integer insuranceExpiringWithinDays,
            @QueryParam("technicalVisitExpiringWithinDays") Integer technicalVisitExpiringWithinDays) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER", "DRIVER");
        return vehicles.search(
                search, status, availableOnly, insuranceExpiringWithinDays, technicalVisitExpiringWithinDays);
    }

    @GET
    @Path("/{id}")
    public VehicleResponse getById(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER", "DRIVER");
        return vehicles.getById(id);
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public VehicleResponse update(@PathParam("id") UUID id, @Valid UpdateVehicleRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return vehicles.update(id, request);
    }

    @PATCH
    @Path("/{id}/deactivate")
    public VehicleResponse deactivate(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return vehicles.deactivate(id);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        vehicles.delete(id);
        return Response.noContent().build();
    }
}
