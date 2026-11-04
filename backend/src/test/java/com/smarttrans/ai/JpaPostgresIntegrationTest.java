package com.smarttrans.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttrans.ai.activity.entity.Activity;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.repository.ActivityRepository;
import com.smarttrans.ai.client.entity.Client;
import com.smarttrans.ai.client.repository.ClientRepository;
import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.driver.enums.DriverStatus;
import com.smarttrans.ai.driver.repository.DriverRepository;
import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.mission.enums.MissionStatus;
import com.smarttrans.ai.mission.repository.MissionRepository;
import com.smarttrans.ai.notification.entity.Notification;
import com.smarttrans.ai.notification.enums.NotificationType;
import com.smarttrans.ai.notification.repository.NotificationRepository;
import com.smarttrans.ai.role.entity.Role;
import com.smarttrans.ai.role.repository.RoleRepository;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import com.smarttrans.ai.vehicle.enums.VehicleType;
import com.smarttrans.ai.vehicle.repository.VehicleRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("it")
@Transactional
class JpaPostgresIntegrationTest {

	private static final EmbeddedPostgres embeddedPostgres;

	static {
		try {
			embeddedPostgres = EmbeddedPostgres.builder().start();
		}
		catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	@AfterAll
	static void stopPostgres() throws IOException {
		embeddedPostgres.close();
	}

	@DynamicPropertySource
	static void registerDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> embeddedPostgres.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "postgres");
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
	}

	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private DriverRepository driverRepository;
	@Autowired
	private VehicleRepository vehicleRepository;
	@Autowired
	private ClientRepository clientRepository;
	@Autowired
	private MissionRepository missionRepository;
	@Autowired
	private NotificationRepository notificationRepository;
	@Autowired
	private ActivityRepository activityRepository;

	@Test
	void flywaySeededRolesAreReadable() {
		assertThat(roleRepository.count()).isEqualTo(3);
		assertThat(roleRepository.findByCode(RoleCode.ADMIN)).isPresent();
		assertThat(roleRepository.findByCode(RoleCode.TRANSPORT_MANAGER)).isPresent();
		assertThat(roleRepository.findByCode(RoleCode.DRIVER)).isPresent();
	}

	@Test
	void fullDomainRoundTripPersistsAndLoadsViaJpa() {
		Role managerRole = roleRepository.findByCode(RoleCode.TRANSPORT_MANAGER).orElseThrow();

		User manager = userRepository.saveAndFlush(User.builder()
				.email("manager@smarttrans.test")
				.passwordHash("$2a$10$dummyhashforintegrationtests000000000000000000")
				.firstName("Alice")
				.lastName("Manager")
				.phone("+212600000001")
				.enabled(true)
				.roles(Set.of(managerRole))
				.build());

		User loadedUser = userRepository.findByEmailWithRoles("manager@smarttrans.test").orElseThrow();
		assertThat(loadedUser.getId()).isEqualTo(manager.getId());
		assertThat(loadedUser.getRoles()).extracting(Role::getCode).containsExactly(RoleCode.TRANSPORT_MANAGER);

		Driver driver = driverRepository.saveAndFlush(Driver.builder()
				.firstName("Karim")
				.lastName("Benali")
				.phone("+212600000002")
				.licenseNumber("PERM-IT-001")
				.licenseExpiresAt(LocalDate.now().plusYears(2))
				.experienceYears((short) 5)
				.status(DriverStatus.AVAILABLE)
				.build());

		Vehicle vehicle = vehicleRepository.saveAndFlush(Vehicle.builder()
				.registrationNumber("12345-A-6")
				.type(VehicleType.TRUCK)
				.capacityKg(new BigDecimal("12000.00"))
				.mileageKm(45000)
				.consumptionLPer100km(new BigDecimal("28.50"))
				.insuranceExpiresAt(LocalDate.now().plusMonths(6))
				.technicalVisitExpiresAt(LocalDate.now().plusMonths(3))
				.status(VehicleStatus.AVAILABLE)
				.build());

		Client client = clientRepository.saveAndFlush(Client.builder()
				.companyName("Atlas Logistics")
				.address("Casablanca, Maroc")
				.phone("+212522000000")
				.email("contact@atlas-logistics.test")
				.build());

		Mission mission = missionRepository.saveAndFlush(Mission.builder()
				.reference("MIS-IT-0001")
				.client(client)
				.driver(driver)
				.vehicle(vehicle)
				.createdBy(manager)
				.departureLocation("Casablanca")
				.destination("Marrakech")
				.scheduledAt(Instant.parse("2026-09-01T08:00:00Z"))
				.weightKg(new BigDecimal("3500.00"))
				.cargoType("Marchandises générales")
				.observations("Livraison matin")
				.status(MissionStatus.PLANIFIED)
				.build());

		assertThat(missionRepository.findByReference("MIS-IT-0001")).isPresent();
		assertThat(missionRepository.findByClient_Id(client.getId())).hasSize(1);
		assertThat(missionRepository.findByDriver_Id(driver.getId())).hasSize(1);
		assertThat(missionRepository.findByVehicle_Id(vehicle.getId())).hasSize(1);
		assertThat(missionRepository.findByStatus(MissionStatus.PLANIFIED))
				.extracting(Mission::getReference)
				.contains("MIS-IT-0001");

		notificationRepository.saveAndFlush(Notification.builder()
				.user(manager)
				.type(NotificationType.MISSION_CREATED)
				.title("Mission créée")
				.body("La mission MIS-IT-0001 a été créée.")
				.mission(mission)
				.build());

		assertThat(notificationRepository.countByUser_IdAndReadAtIsNull(manager.getId())).isEqualTo(1);
		assertThat(notificationRepository.findByUser_IdOrderByCreatedAtDesc(manager.getId())).hasSize(1);

		activityRepository.saveAndFlush(Activity.builder()
				.actor(manager)
				.action(ActivityAction.CREATE)
				.entityType("MISSION")
				.entityId(mission.getId())
				.metadata(Map.of("reference", "MIS-IT-0001", "status", "PLANIFIED"))
				.build());

		assertThat(activityRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("MISSION", mission.getId()))
				.hasSize(1)
				.first()
				.satisfies(activity -> {
					assertThat(activity.getAction()).isEqualTo(ActivityAction.CREATE);
					assertThat(activity.getMetadata()).containsEntry("reference", "MIS-IT-0001");
				});

		assertThat(driverRepository.findByStatus(DriverStatus.AVAILABLE)).isNotEmpty();
		assertThat(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)).isNotEmpty();
		assertThat(clientRepository.findByCompanyNameContainingIgnoreCase("atlas")).hasSize(1);
	}
}
