package com.fortitudetec.elucidation.server.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.accepted;
import static javax.ws.rs.core.Response.ok;

import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.service.RelationshipService;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/")
public class RelationshipResource {

    private RelationshipService service;

    public RelationshipResource(RelationshipService service) {
        this.service = service;
    }

    @POST
    public Response recordEvent(@Valid ConnectionEvent event) {
        service.createEvent(event);
        return accepted().build();
    }

    @Path("/{serviceName}")
    @GET
    public Response viewEventsForService(@PathParam("serviceName") String serviceName) {
        return ok(service.listEventsForService(serviceName)).build();
    }

    @Path("/{serviceName}/relationships")
    @GET
    public Response calculateRelationships(@PathParam("serviceName") String serviceName) {
        return ok(service.buildRelationships(serviceName)).build();
    }
}
