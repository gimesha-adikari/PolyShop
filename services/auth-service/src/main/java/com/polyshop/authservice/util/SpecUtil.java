package com.polyshop.authservice.util;

import org.springframework.data.jpa.domain.Specification;
import java.util.Objects;

public final class SpecUtil {
    private SpecUtil() {}
    public static <T> Specification<T> and(Specification<T> a, Specification<T> b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.and(b);
    }
    public static <T> Specification<T> safeChain(Specification<T> base, Specification<T> next) {
        return and(base, next);
    }
}
