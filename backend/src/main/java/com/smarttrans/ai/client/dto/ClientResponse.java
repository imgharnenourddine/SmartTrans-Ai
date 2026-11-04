package com.smarttrans.ai.client.dto;

import java.time.Instant;
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
public class ClientResponse {

	private UUID id;
	private String companyName;
	private String address;
	private String phone;
	private String email;
	private Instant createdAt;
	private Instant updatedAt;
}
