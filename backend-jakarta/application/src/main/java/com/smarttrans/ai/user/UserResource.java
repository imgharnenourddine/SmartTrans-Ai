package com.smarttrans.ai.user;

import com.smarttrans.ai.security.Access;
import com.smarttrans.ai.user.UserService.CreateUserRequest;
import com.smarttrans.ai.user.UserService.UpdateUserRequest;
import com.smarttrans.ai.user.UserService.UserResponse;
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

@Path("/api/v1/users")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    private UserService users;

    @Inject
    private Access access;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateUserRequest request) {
        access.requireAny("ADMIN");
        return Response.status(Response.Status.CREATED).entity(users.create(request)).build();
    }

    @GET
    @Path("/{id}")
    public UserResponse getById(@PathParam("id") UUID id) {
        access.requireAny("ADMIN");
        return users.getById(id);
    }

    @GET
    public List<UserResponse> getAll() {
        access.requireAny("ADMIN");
        return users.getAll();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public UserResponse update(@PathParam("id") UUID id, @Valid UpdateUserRequest request) {
        access.requireAny("ADMIN");
        return users.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        access.requireAny("ADMIN");
        users.delete(id);
        return Response.noContent().build();
    }
}
