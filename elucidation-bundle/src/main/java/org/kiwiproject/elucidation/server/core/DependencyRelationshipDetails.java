package org.kiwiproject.elucidation.server.core;

import org.kiwiproject.elucidation.common.model.RelationshipDetails;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class DependencyRelationshipDetails {

    private String serviceName;
    private List<RelationshipDetails> details;

}
