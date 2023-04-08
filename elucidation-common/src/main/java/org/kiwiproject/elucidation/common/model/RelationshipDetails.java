package org.kiwiproject.elucidation.common.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RelationshipDetails {

    private String communicationType;
    private String connectionIdentifier;
    private Direction eventDirection;
    private Long lastObserved;

}