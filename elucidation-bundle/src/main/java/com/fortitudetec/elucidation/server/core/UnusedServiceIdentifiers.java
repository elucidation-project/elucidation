package com.fortitudetec.elucidation.server.core;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class UnusedServiceIdentifiers {
    private String serviceName;

    @Builder.Default
    private List<UnusedIdentifier> identifiers = new ArrayList<>();
}
