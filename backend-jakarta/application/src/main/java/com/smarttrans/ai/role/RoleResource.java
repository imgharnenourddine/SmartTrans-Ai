package com.smarttrans.ai.role;

import com.smarttrans.ai.role.RoleService.CreateRoleRequest;
import com.smarttrans.ai.role.RoleService.RoleResponse;
import com.smarttrans.ai.role.RoleService.UpdateRoleRequest;
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

@Path("/api/v1/roles")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class RoleResource {

    @Inject
    private RoleService roles;

    @Inject
    private Access access;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateRoleRequest request) {
        access.requireAny("ADMIN");
        return Response.status(Response.Status.CREATED).entity(roles.create(request)).build();
    }

    @GET
    @Path("/{id}")
    public RoleResponse getById(@PathParam("id") UUID id) {
        access.requireAny("ADMIN");
        return roles.getById(id);
    }

    @GET
    public List<RoleResponse> getAll() {
        access.requireAny("ADMIN");
        return roles.getAll();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public RoleResponse update(@PathParam("id") UUID id, @Valid UpdateRoleRequest request) {
        access.requireAny("ADMIN");
        return roles.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        access.requireAny("ADMIN");
        roles.delete(id);
        return Response.noContent().build();
    }
}
