package com.smarttrans.ai.mission.repository;

import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.mission.enums.MissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionRepository extends JpaRepository<Mission, UUID> {

	Optional<Mission> findByReference(String reference);

	List<Mission> findByStatus(MissionStatus status);

	List<Mission> findByDriver_Id(UUID driverId);

	List<Mission> findAllByOrderByScheduledAtDescReferenceAsc();

	List<Mission> findByDriver_IdOrderByScheduledAtDescReferenceAsc(UUID driverId);

	List<Mission> findByClient_Id(UUID clientId);

	List<Mission> findByVehicle_Id(UUID vehicleId);

	boolean existsByReference(String reference);

	@Query("""
			SELECT m FROM Mission m
			WHERE LOWER(m.reference) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
			   OR LOWER(m.departureLocation) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
			   OR LOWER(m.destination) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
			   OR (m.cargoType IS NOT NULL AND LOWER(m.cargoType) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
			ORDER BY m.scheduledAt DESC, m.reference ASC
			""")
	List<Mission> searchByText(@Param("q") String q);

	@Query("""
			SELECT m FROM Mission m
			WHERE (:search IS NULL OR :search = '' OR
			       LOWER(m.reference) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
			       LOWER(m.departureLocation) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
			       LOWER(m.destination) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
			       LOWER(m.cargoType) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
			  AND (:status IS NULL OR m.status = :status)
			  AND (:driverId IS NULL OR m.driver.id = :driverId)
			  AND (:vehicleId IS NULL OR m.vehicle.id = :vehicleId)
			  AND (:clientId IS NULL OR m.client.id = :clientId)
			  AND (:scheduledFrom IS NULL OR m.scheduledAt >= :scheduledFrom)
			  AND (:scheduledTo IS NULL OR m.scheduledAt <= :scheduledTo)
			ORDER BY m.scheduledAt DESC, m.reference ASC
			""")
	List<Mission> search(
			@Param("search") String search,
			@Param("status") MissionStatus status,
			@Param("driverId") UUID driverId,
			@Param("vehicleId") UUID vehicleId,
			@Param("clientId") UUID clientId,
			@Param("scheduledFrom") Instant scheduledFrom,
			@Param("scheduledTo") Instant scheduledTo);
}
