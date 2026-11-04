package com.smarttrans.ai.driver.dto;

import com.smarttrans.ai.driver.enums.DriverStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDriverRequest {

	private UUID userId;

	@Size(max = 100)
	private String firstName;

	@Size(max = 100)
	private String lastName;

	@Size(max = 30)
	private String phone;

	@Size(max = 50)
	private String licenseNumber;

	private LocalDate licenseExpiresAt;

	@PositiveOrZero
	private Short experienceYears;

	private DriverStatus status;
}
