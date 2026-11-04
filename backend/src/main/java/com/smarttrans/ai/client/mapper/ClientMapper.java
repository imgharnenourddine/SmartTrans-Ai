package com.smarttrans.ai.client.mapper;

import com.smarttrans.ai.client.dto.ClientResponse;
import com.smarttrans.ai.client.dto.CreateClientRequest;
import com.smarttrans.ai.client.dto.UpdateClientRequest;
import com.smarttrans.ai.client.entity.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

	public Client toEntity(CreateClientRequest request) {
		return Client.builder()
				.companyName(request.getCompanyName().trim())
				.address(request.getAddress().trim())
				.phone(request.getPhone().trim())
				.email(normalizeEmail(request.getEmail()))
				.build();
	}

	public void updateEntity(Client client, UpdateClientRequest request) {
		if (request.getCompanyName() != null) {
			client.setCompanyName(request.getCompanyName().trim());
		}
		if (request.getAddress() != null) {
			client.setAddress(request.getAddress().trim());
		}
		if (request.getPhone() != null) {
			client.setPhone(request.getPhone().trim());
		}
		if (request.getEmail() != null) {
			client.setEmail(normalizeEmail(request.getEmail()));
		}
	}

	public ClientResponse toResponse(Client client) {
		return ClientResponse.builder()
				.id(client.getId())
				.companyName(client.getCompanyName())
				.address(client.getAddress())
				.phone(client.getPhone())
				.email(client.getEmail())
				.createdAt(client.getCreatedAt())
				.updatedAt(client.getUpdatedAt())
				.build();
	}

	private String normalizeEmail(String email) {
		if (email == null) {
			return null;
		}
		String trimmed = email.trim().toLowerCase();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
