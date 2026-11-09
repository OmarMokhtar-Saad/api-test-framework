package io.github.omarmokhtarsaad.apitest.example.users;

import io.github.omarmokhtarsaad.apitest.framework.data.TestDataFactory;

/**
 * Valid request bodies with unique, seeded values. Tests start from a valid body and change only the
 * field under test, so each test states exactly what it varies.
 */
public final class UserFactory {

    private UserFactory() {}

    public static CreateUserRequest validMember() {
        return valid(UserRole.MEMBER);
    }

    public static CreateUserRequest valid(UserRole role) {
        String name = TestDataFactory.current().faker().name().fullName();
        return new CreateUserRequest(name, TestDataFactory.uniqueEmail(), role);
    }
}
