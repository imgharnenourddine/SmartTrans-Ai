package com.smarttrans.ai.role.repository;

import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.role.entity.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

	Optional<Role> findByCode(RoleCode code);

	boolean existsByCode(RoleCode code);

	List<Role> findByCodeIn(Collection<RoleCode> codes);
}
