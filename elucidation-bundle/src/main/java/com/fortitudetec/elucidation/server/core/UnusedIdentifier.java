package com.fortitudetec.elucidation.server.core;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UnusedIdentifier {

    private String communicationType;
    private String connectionIdentifier;

}
