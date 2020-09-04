package com.fortitudetec.elucidation.server.core;

import com.fortitudetec.elucidation.common.model.RelationshipDetails;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class DependencyRelationshipDetails {

    private String serviceName;
    private List<RelationshipDetails> details;

}
