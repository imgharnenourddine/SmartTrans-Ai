package com.smarttrans.ai.driver.dto;

import com.smarttrans.ai.driver.enums.DriverStatus;
import jakarta.validation.constraints.NotNull;
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
public class UpdateDriverAvailabilityRequest {

	@NotNull
	private DriverStatus status;
}
