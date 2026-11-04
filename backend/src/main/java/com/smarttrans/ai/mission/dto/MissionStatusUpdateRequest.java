package com.smarttrans.ai.mission.dto;

import com.smarttrans.ai.mission.enums.MissionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MissionStatusUpdateRequest {

	@NotNull
	private MissionStatus status;
}
