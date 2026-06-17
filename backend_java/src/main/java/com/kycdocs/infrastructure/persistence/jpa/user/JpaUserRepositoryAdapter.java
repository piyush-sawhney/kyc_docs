package com.kycdocs.infrastructure.persistence.jpa.user;

import com.kycdocs.domain.user.Email;
import com.kycdocs.domain.user.User;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.domain.user.UserRepository;
import com.kycdocs.domain.user.UserRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springRepo;
    private final UserJpaMapper mapper;

    public JpaUserRepositoryAdapter(SpringDataUserRepository springRepo, UserJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return springRepo.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public List<User> findAllActive() {
        return springRepo.findAllActive().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<User> findAllDeleted() {
        return springRepo.findAllDeleted().stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countActiveByRole(UserRole role) {
        return springRepo.countActiveByRole(role.getValue());
    }

    @Override
    public User save(User user) {
        var entity = mapper.toJpa(user);
        var saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(UserId id) {
        springRepo.deleteById(id.value());
    }
}
