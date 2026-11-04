package com.smarttrans.ai.driver.dto;

import com.smarttrans.ai.driver.enums.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateDriverRequest {

	private UUID userId;

	@NotBlank
	@Size(max = 100)
	private String firstName;

	@NotBlank
	@Size(max = 100)
	private String lastName;

	@NotBlank
	@Size(max = 30)
	private String phone;

	@NotBlank
	@Size(max = 50)
	private String licenseNumber;

	@NotNull
	private LocalDate licenseExpiresAt;

	@PositiveOrZero
	private Short experienceYears;

	private DriverStatus status;
}
