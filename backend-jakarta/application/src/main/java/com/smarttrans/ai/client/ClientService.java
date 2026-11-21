package com.smarttrans.ai.client;

import com.smarttrans.ai.activity.ActivityLog;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.ActivityAction;
import com.smarttrans.ai.domain.Client;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class ClientService {

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    @Inject
    private ActivityLog activityLog;

    public ClientResponse create(CreateClientRequest request) {
        Client client = new Client();
        client.setCompanyName(request.companyName().trim());
        client.setAddress(request.address().trim());
        client.setPhone(request.phone().trim());
        client.setEmail(normalizeEmail(request.email()));
        entityManager.persist(client);
        entityManager.flush();
        activityLog.record(ActivityAction.CREATE, "CLIENT", client.getId(),
                ActivityLog.metadata("companyName", client.getCompanyName()));
        return toResponse(client);
    }

    public ClientResponse getById(UUID id) {
        return toResponse(find(id));
    }

    public List<ClientResponse> getAll() {
        return entityManager.createQuery("select c from Client c", Client.class)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ClientResponse update(UUID id, UpdateClientRequest request) {
        Client client = find(id);
        if (request.companyName() != null) {
            client.setCompanyName(request.companyName().trim());
        }
        if (request.address() != null) {
            client.setAddress(request.address().trim());
        }
        if (request.phone() != null) {
            client.setPhone(request.phone().trim());
        }
        if (request.email() != null) {
            client.setEmail(normalizeEmail(request.email()));
        }
        activityLog.record(ActivityAction.UPDATE, "CLIENT", client.getId(),
                ActivityLog.metadata("companyName", client.getCompanyName()));
        return toResponse(client);
    }

    public void delete(UUID id) {
        Client client = find(id);
        activityLog.record(ActivityAction.DELETE, "CLIENT", client.getId(),
                ActivityLog.metadata("companyName", client.getCompanyName()));
        entityManager.remove(client);
    }

    private Client find(UUID id) {
        Client client = entityManager.find(Client.class, id);
        if (client == null) {
            throw new NotFound("Client not found with id: " + id);
        }
        return client;
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getCompanyName(),
                client.getAddress(),
                client.getPhone(),
                client.getEmail(),
                client.getCreatedAt(),
                client.getUpdatedAt());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record CreateClientRequest(
            @NotBlank @Size(max = 255) String companyName,
            @NotBlank String address,
            @NotBlank @Size(max = 30) String phone,
            @Email @Size(max = 255) String email) {
    }

    public record UpdateClientRequest(
            @Size(max = 255) String companyName,
            String address,
            @Size(max = 30) String phone,
            @Email @Size(max = 255) String email) {
    }

    public record ClientResponse(
            UUID id,
            String companyName,
            String address,
            String phone,
            String email,
            Instant createdAt,
            Instant updatedAt) {
    }
}
