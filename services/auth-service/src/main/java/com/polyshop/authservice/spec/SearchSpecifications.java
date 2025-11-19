package com.polyshop.authservice.spec;

import org.springframework.data.jpa.domain.Specification;

public class SearchSpecifications {
    public static <T> Specification<T> equalsIgnoreCase(String field, String value) {
        return (root, query, builder) ->
                value == null ? null : builder.equal(builder.lower(root.get(field)), value.toLowerCase());
    }
    public static <T> Specification<T> likeIgnoreCase(String field, String value) {
        return (root, query, builder) ->
                value == null ? null : builder.like(builder.lower(root.get(field)), "%" + value.toLowerCase() + "%");
    }
    public static <T> Specification<T> isEqual(String field, Object value) {
        return (root, query, builder) ->
                value == null ? null : builder.equal(root.get(field), value);
    }
    public static <T> Specification<T> isTrue(String field) {
        return (root, query, builder) -> builder.isTrue(root.get(field));
    }
    public static <T> Specification<T> isFalse(String field) {
        return (root, query, builder) -> builder.isFalse(root.get(field));
    }
}
