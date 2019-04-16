package com.fortitudetec.elucidation.server.resources;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 Fortitude Technologies, LLC
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

    @Path("/{serviceName}/relationship/details/{relatedServiceName}")
    @GET
    public Response viewRelationshipDetails(@PathParam("serviceName") String serviceName, @PathParam("relatedServiceName") String relatedServiceName) {
        return Response.ok(service.findRelationshipDetails(serviceName, relatedServiceName)).build();
    }

    @Path("/relationships")
    @GET
    public Response calculateAllDependencies() {
        return Response.ok(service.buildAllDependencies()).build();
    }
}
