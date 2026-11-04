package com.smarttrans.ai.client.service.impl;

import com.smarttrans.ai.activity.ActivityEntityType;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.service.ActivityService;
import com.smarttrans.ai.client.dto.ClientResponse;
import com.smarttrans.ai.client.dto.CreateClientRequest;
import com.smarttrans.ai.client.dto.UpdateClientRequest;
import com.smarttrans.ai.client.entity.Client;
import com.smarttrans.ai.client.mapper.ClientMapper;
import com.smarttrans.ai.client.repository.ClientRepository;
import com.smarttrans.ai.client.service.ClientService;
import com.smarttrans.ai.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

	private final ClientRepository clientRepository;
	private final ClientMapper clientMapper;
	private final ActivityService activityService;

	@Override
	public ClientResponse create(CreateClientRequest request) {
		Client saved = clientRepository.save(clientMapper.toEntity(request));
		activityService.record(
				ActivityAction.CREATE,
				ActivityEntityType.CLIENT,
				saved.getId(),
				ActivityEntityType.metadata("companyName", saved.getCompanyName()));
		return clientMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public ClientResponse getById(UUID id) {
		return clientMapper.toResponse(findClient(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClientResponse> getAll() {
		return clientRepository.findAll().stream().map(clientMapper::toResponse).toList();
	}

	@Override
	public ClientResponse update(UUID id, UpdateClientRequest request) {
		Client client = findClient(id);
		clientMapper.updateEntity(client, request);
		activityService.record(
				ActivityAction.UPDATE,
				ActivityEntityType.CLIENT,
				client.getId(),
				ActivityEntityType.metadata("companyName", client.getCompanyName()));
		return clientMapper.toResponse(client);
	}

	@Override
	public void delete(UUID id) {
		Client client = findClient(id);
		activityService.record(
				ActivityAction.DELETE,
				ActivityEntityType.CLIENT,
				client.getId(),
				ActivityEntityType.metadata("companyName", client.getCompanyName()));
		clientRepository.delete(client);
	}

	private Client findClient(UUID id) {
		return clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Client", id));
	}
}
