package com.polyshop.common.util;

import java.util.UUID;

public final class IdempotencyKeyGenerator {

    private IdempotencyKeyGenerator() {
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
