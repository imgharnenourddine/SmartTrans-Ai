package com.smarttrans.ai.vehicle.repository;

import com.smarttrans.ai.vehicle.entity.Vehicle;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

	Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

	List<Vehicle> findByStatus(VehicleStatus status);

	List<Vehicle> findByInsuranceExpiresAtBefore(LocalDate date);

	List<Vehicle> findByTechnicalVisitExpiresAtBefore(LocalDate date);

	boolean existsByRegistrationNumber(String registrationNumber);

	@Query("""
			SELECT v FROM Vehicle v
			WHERE LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
			ORDER BY v.registrationNumber ASC
			""")
	List<Vehicle> searchByRegistration(@Param("q") String q);

	@Query("""
			SELECT v FROM Vehicle v
			WHERE (:search IS NULL OR :search = '' OR
			       LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
			  AND (:status IS NULL OR v.status = :status)
			  AND (:availableOnly IS NULL OR :availableOnly = FALSE OR
			       (v.status = com.smarttrans.ai.vehicle.enums.VehicleStatus.AVAILABLE
			        AND (v.insuranceExpiresAt IS NULL OR v.insuranceExpiresAt >= :today)
			        AND (v.technicalVisitExpiresAt IS NULL OR v.technicalVisitExpiresAt >= :today)))
			  AND (CAST(:insuranceDeadline AS LocalDate) IS NULL OR
			       (v.insuranceExpiresAt IS NOT NULL AND v.insuranceExpiresAt <= :insuranceDeadline))
			  AND (CAST(:technicalVisitDeadline AS LocalDate) IS NULL OR
			       (v.technicalVisitExpiresAt IS NOT NULL AND v.technicalVisitExpiresAt <= :technicalVisitDeadline))
			ORDER BY v.registrationNumber ASC
			""")
	List<Vehicle> search(
			@Param("search") String search,
			@Param("status") VehicleStatus status,
			@Param("availableOnly") Boolean availableOnly,
			@Param("insuranceDeadline") LocalDate insuranceDeadline,
			@Param("technicalVisitDeadline") LocalDate technicalVisitDeadline,
			@Param("today") LocalDate today);

	@Query("""
			SELECT v FROM Vehicle v
			WHERE v.status <> com.smarttrans.ai.vehicle.enums.VehicleStatus.OUT_OF_SERVICE
			  AND ((v.insuranceExpiresAt IS NOT NULL AND v.insuranceExpiresAt <= :deadline)
			    OR (v.technicalVisitExpiresAt IS NOT NULL AND v.technicalVisitExpiresAt <= :deadline))
			ORDER BY
			    CASE WHEN v.insuranceExpiresAt IS NULL THEN 1 ELSE 0 END,
			    v.insuranceExpiresAt ASC,
			    CASE WHEN v.technicalVisitExpiresAt IS NULL THEN 1 ELSE 0 END,
			    v.technicalVisitExpiresAt ASC,
			    v.registrationNumber ASC
			""")
	List<Vehicle> findWithExpiringDocuments(@Param("deadline") LocalDate deadline);
}
