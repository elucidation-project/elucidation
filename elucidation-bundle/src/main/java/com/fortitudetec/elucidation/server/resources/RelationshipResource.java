package com.fortitudetec.elucidation.server.resources;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 - 2020 Fortitude Technologies, LLC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.server.service.RelationshipService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashSet;
import java.util.OptionalLong;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/elucidate")
public class RelationshipResource {

    private final RelationshipService service;

    public RelationshipResource(RelationshipService service) {
        this.service = service;
    }

    @Path("/event")
    @POST
    public Response recordEvent(@Valid ConnectionEvent event) {
        service.createEvent(event);
        return Response.accepted().build();
    }

    @Path("/events")
    @GET
    public Response viewEventsSince(@NotNull @QueryParam("since") String sinceInMillisParam) {
        var sinceInMillisOptional = parseLong(sinceInMillisParam);

        if (sinceInMillisOptional.isEmpty()) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        var sinceInMillis = sinceInMillisOptional.orElseThrow();
        return Response.ok(service.listEventsSince(sinceInMillis)).build();
    }

    private static OptionalLong parseLong(String value) {
        try {
            return OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    @Path("/service/{serviceName}/events")
    @GET
    public Response viewEventsForService(@PathParam("serviceName") String serviceName) {
        return Response.ok(service.listEventsForService(serviceName)).build();
    }

    @Path("/service/{serviceName}/relationships")
    @GET
    public Response calculateRelationships(@PathParam("serviceName") String serviceName) {
        return Response.ok(service.buildRelationships(serviceName)).build();
    }

    @Path("/service/{serviceName}/relationship/{relatedServiceName}")
    @GET
    public Response viewRelationshipDetails(@PathParam("serviceName") String serviceName, @PathParam("relatedServiceName") String relatedServiceName) {
        return Response.ok(service.findRelationshipDetails(serviceName, relatedServiceName)).build();
    }

    @Path("/services")
    @GET
    public Response currentServiceNames() {
        return Response.ok(new HashSet<>(service.currentServiceNames())).build();
    }

    @Path("/services/details")
    @GET
    public Response currentServiceDetails() {
        return Response.ok(service.currentServiceDetails()).build();
    }

    @Path("/dependencies")
    @GET
    public Response calculateAllDependencies() {
        return Response.ok(service.buildAllDependencies()).build();
    }

    @Path("/dependencies/details")
    @GET
    public Response calculateAllDependenciesWithDetails() {
        return Response.ok(service.buildAllDependenciesWithDetails()).build();
    }

    @Path("/connectionIdentifier/{connectionIdentifier}/events")
    @GET
    public Response viewEventsForConnectionIdentifier(@PathParam("connectionIdentifier") String connectionIdentifier) {
        return Response.ok(service.findAllEventsByConnectionIdentifier(connectionIdentifier)).build();
    }

}
