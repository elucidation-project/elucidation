package com.fortitudetec.elucidation.server.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Connection {

    private String protocol;
    private String identifier;

}
