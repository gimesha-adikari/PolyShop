package com.polyshop.common.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    }
}
