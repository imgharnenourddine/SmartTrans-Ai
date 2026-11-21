package com.smarttrans.ai.driver;

import com.smarttrans.ai.domain.DriverStatus;
import com.smarttrans.ai.driver.DriverService.CreateDriverRequest;
import com.smarttrans.ai.driver.DriverService.DriverResponse;
import com.smarttrans.ai.driver.DriverService.UpdateDriverAvailabilityRequest;
import com.smarttrans.ai.driver.DriverService.UpdateDriverRequest;
import com.smarttrans.ai.security.Access;
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

@Path("/api/v1/drivers")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class DriverResource {

    @Inject
    private DriverService drivers;

    @Inject
    private Access access;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateDriverRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return Response.status(Response.Status.CREATED).entity(drivers.create(request)).build();
    }

    @GET
    @Path("/me")
    public DriverResponse getMe() {
        access.requireAny("DRIVER");
        return drivers.getMe();
    }

    @GET
    @Path("/expiring-licenses")
    public List<DriverResponse> expiringLicenses(@QueryParam("withinDays") @DefaultValue("30") int withinDays) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return drivers.findExpiringLicenses(withinDays);
    }

    @GET
    public List<DriverResponse> search(
            @QueryParam("search") String search,
            @QueryParam("status") DriverStatus status,
            @QueryParam("licenseExpired") Boolean licenseExpired,
            @QueryParam("availableOnly") Boolean availableOnly) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return drivers.search(search, status, licenseExpired, availableOnly);
    }

    @GET
    @Path("/{id}")
    public DriverResponse getById(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER", "DRIVER");
        return drivers.getById(id);
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public DriverResponse update(@PathParam("id") UUID id, @Valid UpdateDriverRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return drivers.update(id, request);
    }

    @PATCH
    @Path("/{id}/availability")
    @Consumes(MediaType.APPLICATION_JSON)
    public DriverResponse updateAvailability(
            @PathParam("id") UUID id, @Valid UpdateDriverAvailabilityRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER", "DRIVER");
        return drivers.updateAvailability(id, request);
    }

    @PATCH
    @Path("/{id}/deactivate")
    public DriverResponse deactivate(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return drivers.deactivate(id);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        drivers.delete(id);
        return Response.noContent().build();
    }
}
