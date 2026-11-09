package io.github.omarmokhtarsaad.apitest.example.users;

import java.time.Instant;

/** A user as returned by the API. Unknown fields are ignored by the shared mapper. */
public record User(long id, String name, String email, UserRole role, Instant createdAt) {}
