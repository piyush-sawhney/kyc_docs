package com.kycdocs.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    List<User> findAllActive();

    List<User> findAllDeleted();

    long countActiveByRole(UserRole role);

    User save(User user);

    void delete(UserId id);
}
