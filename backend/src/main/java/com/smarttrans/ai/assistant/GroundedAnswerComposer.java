package com.smarttrans.ai.assistant;

import com.smarttrans.ai.assistant.dto.AssistantAskResponse;
import com.smarttrans.ai.assistant.dto.AssistantCitation;
import com.smarttrans.ai.client.entity.Client;
import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GroundedAnswerComposer {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

	public AssistantAskResponse compose(AssistantGrounding grounding, String mode, String llmAnswer) {
		List<AssistantCitation> citations = citations(grounding);
		String answer = llmAnswer != null && !llmAnswer.isBlank() ? llmAnswer.trim() : extractive(grounding);
		return AssistantAskResponse.builder()
				.answer(answer)
				.mode(mode)
				.citations(citations)
				.build();
	}

	public String contextBlock(AssistantGrounding grounding) {
		StringBuilder out = new StringBuilder();
		out.append("Question: ").append(grounding.query()).append('\n');
		out.append("Missions totales: ").append(grounding.missionCount())
				.append(" (planifiées: ").append(grounding.planifiedCount())
				.append(", en cours: ").append(grounding.inProgressCount()).append(")\n");
		if (!grounding.expiringDrivers().isEmpty()) {
			out.append("Permis à renouveler:\n");
			for (Driver driver : grounding.expiringDrivers()) {
				out.append("- ").append(driverLine(driver)).append('\n');
			}
		}
		if (!grounding.expiringVehicles().isEmpty()) {
			out.append("Véhicules documents à échéance:\n");
			for (Vehicle vehicle : grounding.expiringVehicles()) {
				out.append("- ").append(vehicleLine(vehicle)).append('\n');
			}
		}
		if (!grounding.unassignedMissions().isEmpty()) {
			out.append("Missions planifiées incomplètes:\n");
			for (Mission mission : grounding.unassignedMissions()) {
				out.append("- ").append(missionLine(mission)).append('\n');
			}
		}
		if (!grounding.availableDrivers().isEmpty()) {
			out.append("Chauffeurs disponibles:\n");
			for (Driver driver : grounding.availableDrivers()) {
				out.append("- ").append(driverLine(driver)).append('\n');
			}
		}
		if (!grounding.availableVehicles().isEmpty()) {
			out.append("Véhicules disponibles:\n");
			for (Vehicle vehicle : grounding.availableVehicles()) {
				out.append("- ").append(vehicleLine(vehicle)).append('\n');
			}
		}
		for (Mission mission : grounding.missions()) {
			out.append("Mission: ").append(missionLine(mission)).append('\n');
		}
		for (Driver driver : grounding.drivers()) {
			out.append("Chauffeur: ").append(driverLine(driver)).append('\n');
		}
		for (Vehicle vehicle : grounding.vehicles()) {
			out.append("Véhicule: ").append(vehicleLine(vehicle)).append('\n');
		}
		for (Client client : grounding.clients()) {
			out.append("Client: ").append(clientLine(client)).append('\n');
		}
		return out.toString();
	}

	private String extractive(AssistantGrounding grounding) {
		String countAnswer = countOnlyAnswer(grounding);
		if (countAnswer != null) {
			return countAnswer;
		}
		boolean hasHits = !grounding.missions().isEmpty()
				|| !grounding.drivers().isEmpty()
				|| !grounding.vehicles().isEmpty()
				|| !grounding.clients().isEmpty()
				|| !grounding.availableDrivers().isEmpty()
				|| !grounding.availableVehicles().isEmpty()
				|| !grounding.unassignedMissions().isEmpty();
		boolean overview = grounding.overview()
				|| (!hasHits && (grounding.missionCount() > 0
						|| !grounding.expiringDrivers().isEmpty()
						|| !grounding.expiringVehicles().isEmpty()));
		if (!hasHits && !overview && !grounding.suggest()) {
			return "Je n’ai pas trouvé d’information dans les missions, chauffeurs, véhicules ou clients pour cette question.";
		}
		StringBuilder answer = new StringBuilder();
		if (grounding.suggest()) {
			answer.append(suggestionBlock(grounding));
		}
		if (overview) {
			answer.append("Voici l’état opérationnel : ")
					.append(grounding.missionCount()).append(" mission(s), dont ")
					.append(grounding.planifiedCount()).append(" planifiée(s) et ")
					.append(grounding.inProgressCount()).append(" en cours. ");
			if (grounding.expiringDrivers().isEmpty()) {
				answer.append("Aucun permis à renouveler dans les 30 jours. ");
			} else {
				answer.append(grounding.expiringDrivers().size())
						.append(" permis à renouveler (30 jours). ");
			}
			if (grounding.expiringVehicles().isEmpty()) {
				answer.append("Aucun véhicule avec assurance ou visite technique à échéance proche.");
			} else {
				answer.append(grounding.expiringVehicles().size())
						.append(" véhicule(s) avec assurance ou visite technique à échéance proche.");
			}
			answer.append('\n');
		}
		appendSection(answer, "Missions", grounding.missions().stream().map(this::missionLine).toList());
		appendSection(answer, "Chauffeurs", grounding.drivers().stream().map(this::driverLine).toList());
		appendSection(answer, "Véhicules", grounding.vehicles().stream().map(this::vehicleLine).toList());
		appendSection(answer, "Clients", grounding.clients().stream().map(this::clientLine).toList());
		return answer.toString().trim();
	}

	private String countOnlyAnswer(AssistantGrounding grounding) {
		String query = grounding.query().toLowerCase();
		if (!containsAny(query, "combien", "nombre")) {
			return null;
		}
		if (containsAny(query, "alerte", "alertes", "expiration", "permis", "risque", "résumé", "resume")) {
			return null;
		}
		if (containsAny(query, "mission")) {
			return frenchCount(grounding.missionCount(), "mission");
		}
		return null;
	}

	private String frenchCount(long count, String singular) {
		if (count <= 1) {
			return count + " " + singular;
		}
		return count + " " + singular + "s";
	}

	private boolean containsAny(String haystack, String... words) {
		for (String word : words) {
			if (haystack.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private String suggestionBlock(AssistantGrounding grounding) {
		StringBuilder answer = new StringBuilder();
		if (grounding.availableDrivers().isEmpty() && grounding.availableVehicles().isEmpty()) {
			answer.append("Aucun chauffeur ni véhicule n’est disponible pour une nouvelle affectation (déjà en mission, ou documents non valides).\n");
			return answer.toString();
		}
		answer.append("Suggestions d’affectation :\n");
		if (!grounding.unassignedMissions().isEmpty()) {
			answer.append("Missions planifiées incomplètes :\n");
			for (Mission mission : grounding.unassignedMissions()) {
				answer.append("- ").append(mission.getReference());
				if (mission.getDriver() == null) {
					answer.append(" · sans chauffeur");
				}
				if (mission.getVehicle() == null) {
					answer.append(" · sans véhicule");
				}
				answer.append('\n');
			}
		}
		if (!grounding.availableDrivers().isEmpty()) {
			Driver first = grounding.availableDrivers().getFirst();
			answer.append("Chauffeur recommandé : ").append(driverName(first))
					.append(" (disponible, permis valide).\n");
		}
		if (!grounding.availableVehicles().isEmpty()) {
			Vehicle first = grounding.availableVehicles().getFirst();
			answer.append("Véhicule recommandé : ").append(first.getRegistrationNumber())
					.append(" (disponible, documents en règle).\n");
		}
		appendSection(answer, "Autres chauffeurs disponibles",
				grounding.availableDrivers().stream().skip(1).map(this::driverLine).toList());
		appendSection(answer, "Autres véhicules disponibles",
				grounding.availableVehicles().stream().skip(1).map(this::vehicleLine).toList());
		return answer.toString();
	}

	private void appendSection(StringBuilder answer, String title, List<String> lines) {
		if (lines.isEmpty()) {
			return;
		}
		answer.append(title).append(" :\n");
		for (String line : lines) {
			answer.append("- ").append(line).append('\n');
		}
	}

	private List<AssistantCitation> citations(AssistantGrounding grounding) {
		Map<String, AssistantCitation> unique = new LinkedHashMap<>();
		for (Mission mission : grounding.missions()) {
			put(unique, "MISSION", mission.getId(), mission.getReference(), missionLine(mission));
		}
		for (Mission mission : grounding.unassignedMissions()) {
			put(unique, "MISSION", mission.getId(), mission.getReference(), missionLine(mission));
		}
		for (Driver driver : grounding.drivers()) {
			put(unique, "DRIVER", driver.getId(), driverName(driver), driverLine(driver));
		}
		for (Driver driver : grounding.expiringDrivers()) {
			put(unique, "DRIVER", driver.getId(), driverName(driver), driverLine(driver));
		}
		for (Driver driver : grounding.availableDrivers()) {
			put(unique, "DRIVER", driver.getId(), driverName(driver), driverLine(driver));
		}
		for (Vehicle vehicle : grounding.vehicles()) {
			put(unique, "VEHICLE", vehicle.getId(), vehicle.getRegistrationNumber(), vehicleLine(vehicle));
		}
		for (Vehicle vehicle : grounding.expiringVehicles()) {
			put(unique, "VEHICLE", vehicle.getId(), vehicle.getRegistrationNumber(), vehicleLine(vehicle));
		}
		for (Vehicle vehicle : grounding.availableVehicles()) {
			put(unique, "VEHICLE", vehicle.getId(), vehicle.getRegistrationNumber(), vehicleLine(vehicle));
		}
		for (Client client : grounding.clients()) {
			put(unique, "CLIENT", client.getId(), client.getCompanyName(), clientLine(client));
		}
		return new ArrayList<>(unique.values());
	}

	private void put(Map<String, AssistantCitation> unique, String kind, UUID id, String title, String snippet) {
		unique.putIfAbsent(kind + ":" + id, AssistantCitation.builder()
				.kind(kind)
				.id(id)
				.title(title)
				.snippet(snippet)
				.build());
	}

	private String missionLine(Mission mission) {
		String client = mission.getClient() != null ? mission.getClient().getCompanyName() : "sans client";
		return mission.getReference() + " · " + mission.getStatus() + " · "
				+ mission.getDepartureLocation() + " → " + mission.getDestination()
				+ " · " + client;
	}

	private String driverLine(Driver driver) {
		return driverName(driver) + " · " + driver.getStatus()
				+ " · permis " + driver.getLicenseNumber()
				+ " expire le " + DAY.format(driver.getLicenseExpiresAt());
	}

	private String driverName(Driver driver) {
		return driver.getFirstName() + " " + driver.getLastName();
	}

	private String vehicleLine(Vehicle vehicle) {
		String insurance = vehicle.getInsuranceExpiresAt() != null
				? DAY.format(vehicle.getInsuranceExpiresAt())
				: "n/a";
		String visit = vehicle.getTechnicalVisitExpiresAt() != null
				? DAY.format(vehicle.getTechnicalVisitExpiresAt())
				: "n/a";
		return vehicle.getRegistrationNumber() + " · " + vehicle.getStatus()
				+ " · assurance " + insurance + " · VT " + visit;
	}

	private String clientLine(Client client) {
		return client.getCompanyName() + " · " + client.getPhone()
				+ (client.getEmail() != null ? " · " + client.getEmail() : "");
	}
}
