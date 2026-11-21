package com.smarttrans.ai.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttrans.ai.config.ExternalConfig;
import com.smarttrans.ai.domain.Client;
import com.smarttrans.ai.domain.Driver;
import com.smarttrans.ai.domain.DriverStatus;
import com.smarttrans.ai.domain.Mission;
import com.smarttrans.ai.domain.MissionStatus;
import com.smarttrans.ai.domain.Vehicle;
import com.smarttrans.ai.domain.VehicleStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
@Transactional
public class AssistantService {

    private static final Set<String> STOPWORDS = Set.of(
            "les", "des", "une", "un", "le", "la", "de", "du", "et", "ou", "pour", "dans", "avec",
            "cette", "cet", "quel", "quels", "quelle", "quelles", "comment", "est", "sont",
            "plus", "moins", "sur", "par", "que", "qui", "quoi", "dont", "the", "a");
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
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

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    @Inject
    private ExternalConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public AskResponse ask(AskRequest request) {
        Grounding grounding = collect(request.message().replace("%", "").replace("_", "").trim());
        String llmAnswer = config.openAiApiKey().isPresent()
                ? complete(SYSTEM_PROMPT, contextBlock(grounding) + "\nRéponds maintenant à la question.").orElse(null)
                : null;
        String mode = llmAnswer != null ? "LLM" : "GROUNDED";
        String answer = llmAnswer != null && !llmAnswer.isBlank() ? llmAnswer.trim() : extractive(grounding);
        return new AskResponse(answer, mode, citations(grounding));
    }

    private java.util.Optional<String> complete(String systemPrompt, String userPrompt) {
        try {
            String base = config.openAiBaseUrl() == null || config.openAiBaseUrl().isBlank()
                    ? "https://api.openai.com/v1"
                    : config.openAiBaseUrl().replaceAll("/$", "");
            String model = config.openAiModel() == null || config.openAiModel().isBlank()
                    ? "gpt-4o-mini"
                    : config.openAiModel();
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt))));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/chat/completions"))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Bearer " + config.openAiApiKey().orElse(""))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().isBlank()) {
                return java.util.Optional.empty();
            }
            JsonNode content = objectMapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(content.asText().trim());
        } catch (Exception ex) {
            return java.util.Optional.empty();
        }
    }

    private Grounding collect(String query) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate deadline = today.plusDays(30);
        boolean overview = mentions(query, "résumé", "resume", "synthèse", "synthese", "dashboard", "alerte", "état", "etat", "situation", "risque");
        boolean suggest = mentions(query, "sugg", "recommand", "affecter", "assign", "disponible", "disponibles", "qui peut", "quel chauffeur", "quel véhicule", "quel vehicule");
        Map<UUID, Mission> missions = new LinkedHashMap<>();
        Map<UUID, Driver> drivers = new LinkedHashMap<>();
        Map<UUID, Vehicle> vehicles = new LinkedHashMap<>();
        Map<UUID, Client> clients = new LinkedHashMap<>();
        for (String needle : needles(query)) {
            for (Mission mission : searchMissions(needle)) {
                missions.putIfAbsent(mission.getId(), mission);
                if (missions.size() >= 8) {
                    break;
                }
            }
            for (Driver driver : searchDrivers(needle)) {
                drivers.putIfAbsent(driver.getId(), driver);
                if (drivers.size() >= 8) {
                    break;
                }
            }
            for (Vehicle vehicle : searchVehicles(needle)) {
                vehicles.putIfAbsent(vehicle.getId(), vehicle);
                if (vehicles.size() >= 8) {
                    break;
                }
            }
            for (Client client : searchClients(needle)) {
                clients.putIfAbsent(client.getId(), client);
                if (clients.size() >= 8) {
                    break;
                }
            }
        }
        List<Driver> expiringDrivers = overview || suggest || mentions(query, "permis", "chauffeur", "licence", "expiration", "risque")
                ? expiringLicenses(deadline)
                : List.of();
        List<Vehicle> expiringVehicles = overview || suggest || mentions(query, "assurance", "visite", "véhicule", "vehicule", "expiration", "risque")
                ? expiringDocuments(deadline)
                : List.of();
        List<Driver> availableDrivers = overview || suggest ? limit(availableDrivers(today), 5) : List.of();
        List<Vehicle> availableVehicles = overview || suggest ? limit(availableVehicles(today), 5) : List.of();
        List<Mission> unassigned = overview || suggest ? unassignedPlanified() : List.of();
        return new Grounding(
                query,
                overview,
                suggest,
                countMissions(null),
                countMissions(MissionStatus.PLANIFIED),
                countMissions(MissionStatus.IN_PROGRESS),
                new ArrayList<>(missions.values()),
                unassigned,
                new ArrayList<>(drivers.values()),
                expiringDrivers,
                availableDrivers,
                new ArrayList<>(vehicles.values()),
                expiringVehicles,
                availableVehicles,
                new ArrayList<>(clients.values()));
    }

    private List<Mission> searchMissions(String needle) {
        return entityManager.createQuery("""
                        SELECT m FROM Mission m
                        WHERE LOWER(m.reference) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                           OR LOWER(m.departureLocation) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                           OR LOWER(m.destination) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                           OR (m.cargoType IS NOT NULL AND LOWER(m.cargoType) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
                        ORDER BY m.scheduledAt DESC, m.reference ASC
                        """, Mission.class)
                .setParameter("q", needle)
                .getResultList();
    }

    private List<Driver> searchDrivers(String needle) {
        return entityManager.createQuery("""
                        SELECT d FROM Driver d
                        WHERE LOWER(d.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                           OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                           OR LOWER(d.phone) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                           OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                        ORDER BY d.lastName ASC, d.firstName ASC
                        """, Driver.class)
                .setParameter("search", needle)
                .getResultList();
    }

    private List<Vehicle> searchVehicles(String needle) {
        return entityManager.createQuery("""
                        SELECT v FROM Vehicle v
                        WHERE LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                        ORDER BY v.registrationNumber ASC
                        """, Vehicle.class)
                .setParameter("q", needle)
                .getResultList();
    }

    private List<Client> searchClients(String needle) {
        return entityManager.createQuery("""
                        SELECT c FROM Client c
                        WHERE LOWER(c.companyName) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))
                        """, Client.class)
                .setParameter("name", needle)
                .getResultList();
    }

    private List<Driver> expiringLicenses(LocalDate deadline) {
        return entityManager.createQuery("""
                        SELECT d FROM Driver d
                        WHERE d.licenseExpiresAt <= :deadline
                          AND d.status <> com.smarttrans.ai.domain.DriverStatus.SUSPENDED
                        ORDER BY d.licenseExpiresAt ASC, d.lastName ASC, d.firstName ASC
                        """, Driver.class)
                .setParameter("deadline", deadline)
                .getResultList();
    }

    private List<Vehicle> expiringDocuments(LocalDate deadline) {
        return entityManager.createQuery("""
                        SELECT v FROM Vehicle v
                        WHERE v.status <> com.smarttrans.ai.domain.VehicleStatus.OUT_OF_SERVICE
                          AND ((v.insuranceExpiresAt IS NOT NULL AND v.insuranceExpiresAt <= :deadline)
                            OR (v.technicalVisitExpiresAt IS NOT NULL AND v.technicalVisitExpiresAt <= :deadline))
                        ORDER BY
                            CASE WHEN v.insuranceExpiresAt IS NULL THEN 1 ELSE 0 END,
                            v.insuranceExpiresAt ASC,
                            CASE WHEN v.technicalVisitExpiresAt IS NULL THEN 1 ELSE 0 END,
                            v.technicalVisitExpiresAt ASC,
                            v.registrationNumber ASC
                        """, Vehicle.class)
                .setParameter("deadline", deadline)
                .getResultList();
    }

    private List<Driver> availableDrivers(LocalDate today) {
        return entityManager.createQuery("""
                        SELECT d FROM Driver d
                        WHERE d.status = :status
                          AND d.licenseExpiresAt >= :today
                        ORDER BY d.lastName ASC, d.firstName ASC
                        """, Driver.class)
                .setParameter("status", DriverStatus.AVAILABLE)
                .setParameter("today", today)
                .getResultList();
    }

    private List<Vehicle> availableVehicles(LocalDate today) {
        return entityManager.createQuery("""
                        SELECT v FROM Vehicle v
                        WHERE v.status = :status
                          AND (v.insuranceExpiresAt IS NULL OR v.insuranceExpiresAt >= :today)
                          AND (v.technicalVisitExpiresAt IS NULL OR v.technicalVisitExpiresAt >= :today)
                        ORDER BY v.registrationNumber ASC
                        """, Vehicle.class)
                .setParameter("status", VehicleStatus.AVAILABLE)
                .setParameter("today", today)
                .getResultList();
    }

    private long countMissions(MissionStatus status) {
        if (status == null) {
            return entityManager.createQuery("select count(m) from Mission m", Long.class).getSingleResult();
        }
        return entityManager.createQuery("select count(m) from Mission m where m.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    private List<Mission> unassignedPlanified() {
        List<Mission> result = new ArrayList<>();
        List<Mission> planned = entityManager.createQuery(
                        "select m from Mission m where m.status = :status", Mission.class)
                .setParameter("status", MissionStatus.PLANIFIED)
                .getResultList();
        for (Mission mission : planned) {
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

    private boolean mentions(String query, String... words) {
        String haystack = query.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (haystack.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String contextBlock(Grounding grounding) {
        StringBuilder out = new StringBuilder();
        out.append("Question: ").append(grounding.query()).append('\n');
        out.append("Missions totales: ").append(grounding.missionCount())
                .append(" (planifiées: ").append(grounding.planifiedCount())
                .append(", en cours: ").append(grounding.inProgressCount()).append(")\n");
        appendLines(out, "Permis à renouveler:", grounding.expiringDrivers().stream().map(this::driverLine).toList());
        appendLines(out, "Véhicules documents à échéance:", grounding.expiringVehicles().stream().map(this::vehicleLine).toList());
        appendLines(out, "Missions planifiées incomplètes:", grounding.unassignedMissions().stream().map(this::missionLine).toList());
        appendLines(out, "Chauffeurs disponibles:", grounding.availableDrivers().stream().map(this::driverLine).toList());
        appendLines(out, "Véhicules disponibles:", grounding.availableVehicles().stream().map(this::vehicleLine).toList());
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

    private void appendLines(StringBuilder out, String title, List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        out.append(title).append('\n');
        for (String line : lines) {
            out.append("- ").append(line).append('\n');
        }
    }

    private String extractive(Grounding grounding) {
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
                answer.append(grounding.expiringDrivers().size()).append(" permis à renouveler (30 jours). ");
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

    private String countOnlyAnswer(Grounding grounding) {
        String query = grounding.query().toLowerCase(Locale.ROOT);
        if (!containsAny(query, "combien", "nombre")) {
            return null;
        }
        if (containsAny(query, "alerte", "alertes", "expiration", "permis", "risque", "résumé", "resume")) {
            return null;
        }
        if (containsAny(query, "mission")) {
            long count = grounding.missionCount();
            return count <= 1 ? count + " mission" : count + " missions";
        }
        return null;
    }

    private boolean containsAny(String haystack, String... words) {
        for (String word : words) {
            if (haystack.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String suggestionBlock(Grounding grounding) {
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

    private List<Citation> citations(Grounding grounding) {
        Map<String, Citation> unique = new LinkedHashMap<>();
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

    private void put(Map<String, Citation> unique, String kind, UUID id, String title, String snippet) {
        unique.putIfAbsent(kind + ":" + id, new Citation(kind, id, title, snippet));
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
        String insurance = vehicle.getInsuranceExpiresAt() != null ? DAY.format(vehicle.getInsuranceExpiresAt()) : "n/a";
        String visit = vehicle.getTechnicalVisitExpiresAt() != null ? DAY.format(vehicle.getTechnicalVisitExpiresAt()) : "n/a";
        return vehicle.getRegistrationNumber() + " · " + vehicle.getStatus()
                + " · assurance " + insurance + " · VT " + visit;
    }

    private String clientLine(Client client) {
        return client.getCompanyName() + " · " + client.getPhone()
                + (client.getEmail() != null ? " · " + client.getEmail() : "");
    }

    public record AskRequest(@NotBlank @Size(min = 2, max = 2000) String message) {
    }

    public record Citation(String kind, UUID id, String title, String snippet) {
    }

    public record AskResponse(String answer, String mode, List<Citation> citations) {
    }

    private record Grounding(
            String query,
            boolean overview,
            boolean suggest,
            long missionCount,
            long planifiedCount,
            long inProgressCount,
            List<Mission> missions,
            List<Mission> unassignedMissions,
            List<Driver> drivers,
            List<Driver> expiringDrivers,
            List<Driver> availableDrivers,
            List<Vehicle> vehicles,
            List<Vehicle> expiringVehicles,
            List<Vehicle> availableVehicles,
            List<Client> clients) {
    }
}
