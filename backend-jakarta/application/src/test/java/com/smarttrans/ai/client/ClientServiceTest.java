package com.smarttrans.ai.client;

import com.smarttrans.ai.ServiceTestSupport;
import com.smarttrans.ai.activity.ActivityLog;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.client.ClientService.CreateClientRequest;
import com.smarttrans.ai.client.ClientService.UpdateClientRequest;
import com.smarttrans.ai.domain.ActivityAction;
import com.smarttrans.ai.domain.Client;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientServiceTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final ActivityLog activityLog = mock(ActivityLog.class);
    private ClientService clients;

    @BeforeEach
    void setUp() {
        clients = new ClientService();
        ServiceTestSupport.inject(clients, entityManager, activityLog);
        ServiceTestSupport.stubQueries(entityManager, Map.of());
    }

    @Test
    void createTrimsFieldsAndNormalizesEmail() {
        var response = clients.create(new CreateClientRequest(
                "  Atlas Freight  ",
                "  1 rue de Rabat  ",
                "  0600000001  ",
                "  Ops@Client.COM  "));

        assertEquals("Atlas Freight", response.companyName());
        assertEquals("1 rue de Rabat", response.address());
        assertEquals("0600000001", response.phone());
        assertEquals("ops@client.com", response.email());
        verify(activityLog).record(
                eq(ActivityAction.CREATE),
                eq("CLIENT"),
                isNull(),
                eq(ActivityLog.metadata("companyName", "Atlas Freight")));
    }

    @Test
    void blankEmailIsStoredAsNull() {
        var response = clients.create(new CreateClientRequest("Atlas", "Rabat", "0600", "   "));

        assertNull(response.email());
    }

    @Test
    void missingClientIsNotFound() {
        UUID id = UUID.randomUUID();

        NotFound error = assertThrows(NotFound.class, () -> clients.getById(id));

        assertEquals("Client not found with id: " + id, error.getMessage());
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        Client client = client("Atlas", "Rabat", "0600", "atlas@client.com");
        when(entityManager.find(Client.class, id)).thenReturn(client);

        var response = clients.update(id, new UpdateClientRequest(null, null, "  0700  ", null));

        assertEquals("Atlas", response.companyName());
        assertEquals("Rabat", response.address());
        assertEquals("0700", response.phone());
        assertEquals("atlas@client.com", response.email());
        verify(activityLog).record(
                eq(ActivityAction.UPDATE),
                eq("CLIENT"),
                isNull(),
                eq(ActivityLog.metadata("companyName", "Atlas")));
    }

    @Test
    void deleteRecordsTheActivityThenRemovesTheClient() {
        UUID id = UUID.randomUUID();
        Client client = client("Atlas", "Rabat", "0600", "atlas@client.com");
        when(entityManager.find(Client.class, id)).thenReturn(client);

        clients.delete(id);

        verify(activityLog).record(
                eq(ActivityAction.DELETE),
                eq("CLIENT"),
                isNull(),
                eq(ActivityLog.metadata("companyName", "Atlas")));
        verify(entityManager).remove(client);
    }

    private static Client client(String company, String address, String phone, String email) {
        Client client = new Client();
        client.setCompanyName(company);
        client.setAddress(address);
        client.setPhone(phone);
        client.setEmail(email);
        return client;
    }
}
