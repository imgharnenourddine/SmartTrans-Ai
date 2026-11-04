package com.smarttrans.ai.user.repository;

import com.smarttrans.ai.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	@Query("select u from User u left join fetch u.roles where u.email = :email")
	Optional<User> findByEmailWithRoles(@Param("email") String email);

	@Query("select u from User u left join fetch u.roles where u.id = :id")
	Optional<User> findByIdWithRoles(@Param("id") UUID id);

	@Query("select distinct u from User u left join fetch u.roles")
	List<User> findAllWithRoles();
}
