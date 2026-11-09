package io.github.omarmokhtarsaad.apitest.example.users;

/**
 * Request body for creating or replacing a user. Null fields are omitted from the JSON, which is how
 * negative tests send an incomplete body.
 */
public record CreateUserRequest(String name, String email, UserRole role) {

    public CreateUserRequest withEmail(String newEmail) {
        return new CreateUserRequest(name, newEmail, role);
    }

    public CreateUserRequest withName(String newName) {
        return new CreateUserRequest(newName, email, role);
    }
}
