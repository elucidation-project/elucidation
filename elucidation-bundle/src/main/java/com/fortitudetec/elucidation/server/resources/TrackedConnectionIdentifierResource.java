package com.fortitudetec.elucidation.server.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.server.service.TrackedConnectionIdentifierService;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/elucidate")
public class TrackedConnectionIdentifierResource {

    private final TrackedConnectionIdentifierService service;

    public TrackedConnectionIdentifierResource(TrackedConnectionIdentifierService service) {
        this.service = service;
    }

    @POST
    @Path("/trackedIdentifier/{serviceName}/{communicationType}")
    @Timed
    @ExceptionMetered
    public Response loadTrackedIdentifiers(@PathParam("serviceName") String serviceName,
                                           @PathParam("communicationType") String communicationType,
                                           @NotEmpty List<@NotBlank String> connectionIdentifiers) {

        service.loadNewIdentifiers(serviceName, communicationType, connectionIdentifiers);
        return Response.accepted().build();
    }

    @GET
    @Path("/connectionIdentifier/unused")
    @Timed
    @ExceptionMetered
    public Response findUnusedIdentifiers() {
        var unused = service.findUnusedIdentifiers();

        return Response.ok(unused).build();
    }

    @GET
    @Path("/trackedIdentifiers")
    @Timed
    @ExceptionMetered
    public Response allTrackedIdentifiers() {
        return Response.ok(service.allTrackedConnectionIdentifiers()).build();
    }

}
