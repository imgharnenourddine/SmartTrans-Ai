package com.smarttrans.ai.assistant.service.impl;

import com.smarttrans.ai.assistant.AssistantGrounding;
import com.smarttrans.ai.assistant.GroundedAnswerComposer;
import com.smarttrans.ai.assistant.OpenAiChatClient;
import com.smarttrans.ai.assistant.dto.AssistantAskRequest;
import com.smarttrans.ai.assistant.dto.AssistantAskResponse;
import com.smarttrans.ai.assistant.service.AssistantService;
import com.smarttrans.ai.client.entity.Client;
import com.smarttrans.ai.client.repository.ClientRepository;
import com.smarttrans.ai.driver.entity.Driver;
import com.smarttrans.ai.driver.enums.DriverStatus;
import com.smarttrans.ai.driver.repository.DriverRepository;
import com.smarttrans.ai.mission.entity.Mission;
import com.smarttrans.ai.mission.enums.MissionStatus;
import com.smarttrans.ai.mission.repository.MissionRepository;
import com.smarttrans.ai.vehicle.entity.Vehicle;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import com.smarttrans.ai.vehicle.repository.VehicleRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssistantServiceImpl implements AssistantService {

	private static final Set<String> STOPWORDS = Set.of(
			"les", "des", "une", "un", "le", "la", "de", "du", "et", "ou", "pour", "dans", "avec",
			"cette", "cet", "quel", "quels", "quelle", "quelles", "comment", "est", "sont",
			"plus", "moins", "sur", "par", "que", "qui", "quoi", "dont", "the", "a");
	private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
	private static final String SYSTEM_PROMPT = """
			Tu es l'assistant SmartTrans, plateforme de gestion transport.
			Réponds uniquement à partir du contexte fourni (missions, chauffeurs, véhicules, clients).
			Si on te demande un décompte (combien), réponds uniquement par le nombre et le mot
			(ex. « 1 mission »), sans résumé opérationnel.
			Si on te demande une recommandation, propose un chauffeur AVAILABLE (permis valide)
			et un véhicule AVAILABLE (assurance et visite technique valides). Justifie en une phrase.
			N'invente aucun nom hors contexte. Si l'information n'y figure pas, dis-le clairement.
			Réponds en français, de façon concise.
			""";

	private final MissionRepository missionRepository;
	private final DriverRepository driverRepository;
	private final VehicleRepository vehicleRepository;
	private final ClientRepository clientRepository;
	private final GroundedAnswerComposer composer;
	private final OpenAiChatClient openAiChatClient;

	@Override
	public AssistantAskResponse ask(AssistantAskRequest request) {
		AssistantGrounding grounding = collect(sanitize(request.getMessage()));
		String llmAnswer = openAiChatClient.enabled()
				? openAiChatClient.complete(SYSTEM_PROMPT, composer.contextBlock(grounding) + "\nRéponds maintenant à la question.").orElse(null)
				: null;
		String mode = llmAnswer != null ? "LLM" : "GROUNDED";
		return composer.compose(grounding, mode, llmAnswer);
	}

	private AssistantGrounding collect(String query) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate deadline = today.plusDays(30);
		boolean overview = isOverview(query);
		boolean suggest = isSuggest(query);
		List<String> needles = needles(query);

		Map<UUID, Mission> missions = new LinkedHashMap<>();
		Map<UUID, Driver> drivers = new LinkedHashMap<>();
		Map<UUID, Vehicle> vehicles = new LinkedHashMap<>();
		Map<UUID, Client> clients = new LinkedHashMap<>();

		for (String needle : needles) {
			for (Mission mission : missionRepository.searchByText(needle)) {
				touchMission(mission);
				missions.putIfAbsent(mission.getId(), mission);
				if (missions.size() >= 8) {
					break;
				}
			}
			for (Driver driver : driverRepository.search(needle, null, null, null, today)) {
				drivers.putIfAbsent(driver.getId(), driver);
				if (drivers.size() >= 8) {
					break;
				}
			}
			for (Vehicle vehicle : vehicleRepository.searchByRegistration(needle)) {
				vehicles.putIfAbsent(vehicle.getId(), vehicle);
				if (vehicles.size() >= 8) {
					break;
				}
			}
			for (Client client : clientRepository.findByCompanyNameContainingIgnoreCase(needle)) {
				clients.putIfAbsent(client.getId(), client);
				if (clients.size() >= 8) {
					break;
				}
			}
		}

		List<Driver> expiringDrivers = overview || suggest
				|| mentions(query, "permis", "chauffeur", "licence", "expiration", "risque")
				? driverRepository.findExpiringLicenses(deadline)
				: List.of();
		List<Vehicle> expiringVehicles = overview || suggest
				|| mentions(query, "assurance", "visite", "véhicule", "vehicule", "expiration", "risque")
				? vehicleRepository.findWithExpiringDocuments(deadline)
				: List.of();

		List<Driver> availableDrivers = overview || suggest
				? limit(driverRepository.search(null, DriverStatus.AVAILABLE, false, true, today), 5)
				: List.of();
		List<Vehicle> availableVehicles = overview || suggest
				? limit(vehicleRepository.search(null, VehicleStatus.AVAILABLE, true, null, null, today), 5)
				: List.of();
		List<Mission> unassignedMissions = overview || suggest
				? unassignedPlanified()
				: List.of();

		long missionCount = missionRepository.count();
		long planified = missionRepository.findByStatus(MissionStatus.PLANIFIED).size();
		long inProgress = missionRepository.findByStatus(MissionStatus.IN_PROGRESS).size();

		return new AssistantGrounding(
				query,
				overview,
				suggest,
				missionCount,
				planified,
				inProgress,
				new ArrayList<>(missions.values()),
				unassignedMissions,
				new ArrayList<>(drivers.values()),
				expiringDrivers,
				availableDrivers,
				new ArrayList<>(vehicles.values()),
				expiringVehicles,
				availableVehicles,
				new ArrayList<>(clients.values()));
	}

	private List<Mission> unassignedPlanified() {
		List<Mission> result = new ArrayList<>();
		for (Mission mission : missionRepository.findByStatus(MissionStatus.PLANIFIED)) {
			touchMission(mission);
			if (mission.getDriver() == null || mission.getVehicle() == null) {
				result.add(mission);
			}
			if (result.size() >= 5) {
				break;
			}
		}
		return result;
	}

	private <T> List<T> limit(List<T> values, int max) {
		if (values.size() <= max) {
			return values;
		}
		return values.subList(0, max);
	}

	private void touchMission(Mission mission) {
		if (mission.getClient() != null) {
			mission.getClient().getCompanyName();
		}
		if (mission.getDriver() != null) {
			mission.getDriver().getLastName();
		}
		if (mission.getVehicle() != null) {
			mission.getVehicle().getRegistrationNumber();
		}
	}

	private List<String> needles(String query) {
		List<String> values = new ArrayList<>();
		if (query.length() >= 2 && query.split("\\s+").length <= 4) {
			values.add(query);
		}
		TOKEN.matcher(query.toLowerCase(Locale.ROOT)).results()
				.map(match -> match.group())
				.filter(token -> token.length() >= 3 && !STOPWORDS.contains(token))
				.forEach(token -> {
					if (!values.contains(token)) {
						values.add(token);
					}
				});
		if (values.isEmpty() && query.length() >= 2) {
			values.add(query);
		}
		return values;
	}

	private boolean isOverview(String query) {
		return mentions(query, "résumé", "resume", "synthèse", "synthese",
				"dashboard", "alerte", "état", "etat", "situation", "risque");
	}

	private boolean isSuggest(String query) {
		return mentions(query, "sugg", "recommand", "affecter", "assign", "disponible",
				"disponibles", "qui peut", "quel chauffeur", "quel véhicule", "quel vehicule");
	}

	private boolean mentions(String query, String... words) {
		String haystack = query.toLowerCase(Locale.ROOT);
		for (String word : words) {
			if (haystack.contains(word.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private String sanitize(String query) {
		return query.replace("%", "").replace("_", "").trim();
	}
}
