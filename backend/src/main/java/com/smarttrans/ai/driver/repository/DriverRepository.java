package com.smarttrans.ai.driver.repository;

import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.driver.enums.DriverStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

	Optional<Driver> findByLicenseNumber(String licenseNumber);

	Optional<Driver> findByUser_Id(UUID userId);

	List<Driver> findByStatus(DriverStatus status);

	boolean existsByLicenseNumber(String licenseNumber);

	boolean existsByUser_Id(UUID userId);

	@Query("""
			SELECT d FROM Driver d
			WHERE (:search IS NULL OR :search = '' OR
			       LOWER(d.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
			       LOWER(d.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
			       LOWER(d.phone) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
			       LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
			  AND (:status IS NULL OR d.status = :status)
			  AND (:licenseExpired IS NULL OR
			       (:licenseExpired = TRUE AND d.licenseExpiresAt < :today) OR
			       (:licenseExpired = FALSE AND d.licenseExpiresAt >= :today))
			  AND (:availableOnly IS NULL OR :availableOnly = FALSE OR
			       (d.status = com.smarttrans.ai.driver.enums.DriverStatus.AVAILABLE AND d.licenseExpiresAt >= :today))
			ORDER BY d.lastName ASC, d.firstName ASC
			""")
	List<Driver> search(
			@Param("search") String search,
			@Param("status") DriverStatus status,
			@Param("licenseExpired") Boolean licenseExpired,
			@Param("availableOnly") Boolean availableOnly,
			@Param("today") LocalDate today);

	@Query("""
			SELECT d FROM Driver d
			WHERE d.licenseExpiresAt <= :deadline
			  AND d.status <> com.smarttrans.ai.driver.enums.DriverStatus.SUSPENDED
			ORDER BY d.licenseExpiresAt ASC, d.lastName ASC, d.firstName ASC
			""")
	List<Driver> findExpiringLicenses(@Param("deadline") LocalDate deadline);
}
