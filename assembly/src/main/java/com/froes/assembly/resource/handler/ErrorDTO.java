package com.froes.assembly.resource.handler;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;


@Value
@With
@Jacksonized
@Builder
public class ErrorDTO {
    @Builder.Default
    OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);
    int status;
    String error;
    String message;
    String path;
}
