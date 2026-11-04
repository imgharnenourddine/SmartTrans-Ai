package com.smarttrans.ai.client.repository;

import com.smarttrans.ai.client.entity.Client;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, UUID> {

	List<Client> findByCompanyNameContainingIgnoreCase(String companyName);
}
