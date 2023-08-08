package org.kiwiproject.elucidation.server.resources;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.kiwiproject.elucidation.server.service.TrackedConnectionIdentifierService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
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

    @GET
    @Path("/connectionIdentifier/{serviceName}/unused")
    @Timed
    @ExceptionMetered
    public Response findUnusedIdentifiersForService(@PathParam("serviceName") String serviceName) {
        return Response.ok(service.findUnusedIdentifiersForService(serviceName)).build();
    }

}
