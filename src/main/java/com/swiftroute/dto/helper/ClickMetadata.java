package com.swiftroute.dto.helper;

import com.swiftroute.entity.Url;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ClickMetadata {
    private final Url url;
    private final String ipAddress;
    private final String userAgent;
    private final String referrer;
}
