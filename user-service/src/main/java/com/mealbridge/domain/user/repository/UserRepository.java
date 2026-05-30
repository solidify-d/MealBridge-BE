package com.mealbridge.domain.user.repository;

import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	List<User> findByRole(Role role);

	List<User> findByStatus(UserStatus status);

	List<User> findByStatusAndRoleIn(UserStatus status, List<Role> roles);
}
