package com.polyshop.authservice.spec;

import com.polyshop.authservice.domain.User;
import com.polyshop.authservice.domain.enums.Status;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecs {
    public static Specification<User> hasEmail(String email) {
        return SearchSpecifications.equalsIgnoreCase("email", email);
    }
    public static Specification<User> hasUsername(String username) {
        return SearchSpecifications.equalsIgnoreCase("username", username);
    }
    public static Specification<User> hasPhone(String phone) {
        return SearchSpecifications.equalsIgnoreCase("phone", phone);
    }
    public static Specification<User> hasStatus(Status status) {
        return SearchSpecifications.isEqual("status", status);
    }
    public static Specification<User> emailVerified(boolean verified) {
        return verified ? SearchSpecifications.isTrue("emailVerified") : SearchSpecifications.isFalse("emailVerified");
    }
    public static Specification<User> phoneVerified(boolean verified) {
        return verified ? SearchSpecifications.isTrue("phoneVerified") : SearchSpecifications.isFalse("phoneVerified");
    }
}
