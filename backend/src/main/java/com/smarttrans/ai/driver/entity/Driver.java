package com.smarttrans.ai.driver.entity;

import com.smarttrans.ai.common.entity.AuditableEntity;
import com.smarttrans.ai.driver.enums.DriverStatus;
import com.smarttrans.ai.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", unique = true)
	private User user;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, length = 30)
	private String phone;

	@Column(name = "license_number", nullable = false, unique = true, length = 50)
	private String licenseNumber;

	@Column(name = "license_expires_at", nullable = false)
	private LocalDate licenseExpiresAt;

	@Column(name = "experience_years")
	private Short experienceYears;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private DriverStatus status;
}
