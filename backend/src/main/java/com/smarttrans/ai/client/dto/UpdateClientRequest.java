package com.smarttrans.ai.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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
public class UpdateClientRequest {

	@Size(max = 255)
	private String companyName;

	private String address;

	@Size(max = 30)
	private String phone;

	@Email
	@Size(max = 255)
	private String email;
}
