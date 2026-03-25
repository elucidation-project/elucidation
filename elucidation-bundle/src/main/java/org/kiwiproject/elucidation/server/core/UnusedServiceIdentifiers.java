package org.kiwiproject.elucidation.server.core;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class UnusedServiceIdentifiers {
    private final String serviceName;

    @Builder.Default
    private final List<UnusedIdentifier> identifiers = new ArrayList<>();
}
