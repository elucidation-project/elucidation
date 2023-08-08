package org.kiwiproject.elucidation.client.helper.app;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/dummy")
public class DummyResource {

    @GET
    public Response getMethod() {
        return Response.ok().build();
    }

    @POST
    @Path("/post")
    public Response postMethod() {
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}")
    public Response putMethod(@PathParam("id") String id) {
        return Response.ok().build();
    }
}
