package org.kiwiproject.elucidation.client.helper.app;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

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
