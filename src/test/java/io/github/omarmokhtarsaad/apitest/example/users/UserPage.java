package io.github.omarmokhtarsaad.apitest.example.users;

import java.util.List;

/** One page of {@code GET /users}. */
public record UserPage(int page, int size, int total, List<User> items) {

    public UserPage {
        items = List.copyOf(items);
    }
}
