package com.spring.camelsvsdwarfs.config;

import com.spring.camelsvsdwarfs.entity.*;
import com.spring.camelsvsdwarfs.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Carga los datos minimos exigidos por el enunciado (seccion 8):
 * 3 usuarios, 5 enanos, 2 camellos, 2 medianos, 2 equipos,
 * 3 carreras en estados distintos y 1 carrera COMPLETED con resultados.
 *
 * Los UUID de los usuarios coinciden con los "id" fijos del realm de Keycloak
 * (realm-camelsvsdwarfs.json), de modo que el claim "sub" del JWT siempre
 * corresponde al mismo registro local aunque se recree el contenedor.
 *
 * Es idempotente: si ya hay competidores en la base de datos, no hace nada.
 * No corre en el perfil de tests.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_KC_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ORGANIZER_KC_ID = "22222222-2222-2222-2222-222222222222";
    private static final String VIEWER_KC_ID = "33333333-3333-3333-3333-333333333333";

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RaceRepository raceRepository;
    private final RegisterPlayerRepository registerPlayerRepository;
    private final StandingResultRepository standingResultRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (playerRepository.count() > 0) {
            log.info("Datos semilla ya presentes, no se vuelve a sembrar.");
            return;
        }

        log.info("Sembrando datos iniciales...");

        // ---------- Usuarios (espejo local de las identidades de Keycloak) ----------
        User admin = saveUser(ADMIN_KC_ID, "admin", "admin@camelsvsdwarfs.test");
        User organizer = saveUser(ORGANIZER_KC_ID, "organizer", "organizer@camelsvsdwarfs.test");
        saveUser(VIEWER_KC_ID, "viewer", "viewer@camelsvsdwarfs.test");

        // ---------- Competidores ----------
        Player nullPointer = savePlayer("Null Pointer", "nullpointer", PlayerType.DWARF,
                LocalDate.of(1995, 3, 12), "1.30", "62.50", "Zuniga, Envigado");
        Player stackOverflow = savePlayer("Stack Overflow", "stackoverflow", PlayerType.DWARF,
                LocalDate.of(1993, 7, 4), "1.28", "65.00", "Alto de Las Palmas");
        Player littleLambda = savePlayer("Little Lambda", "littlelambda", PlayerType.DWARF,
                LocalDate.of(1998, 11, 22), "1.25", "58.30", "Sabaneta");
        Player captainCache = savePlayer("Captain Cache", "captaincache", PlayerType.DWARF,
                LocalDate.of(1991, 1, 30), "1.33", "70.10", "Rionegro");
        Player tinyDocker = savePlayer("Tiny Docker", "tinydocker", PlayerType.DWARF,
                LocalDate.of(2000, 5, 17), "1.20", "55.00", "Itagui");

        Player byteCamel = savePlayer("Byte", "byte", PlayerType.CAMEL,
                LocalDate.of(2016, 2, 29), "2.15", "480.00", "Desierto de la Tatacoa");
        Player kernelPanic = savePlayer("Kernel Panic", "kernelpanic", PlayerType.CAMEL,
                LocalDate.of(2017, 9, 9), "2.05", "455.75", "La Guajira");

        savePlayer("Merge Conflict", "mergeconflict", PlayerType.MEDIUM,
                LocalDate.of(2001, 8, 8), "1.70", "80.00", "Medellin");
        savePlayer("Race Condition", "racecondition", PlayerType.MEDIUM,
                LocalDate.of(1999, 12, 1), "1.75", "83.40", "Envigado");

        // ---------- Equipos ----------
        Team fiveExceptions = saveTeam("The Five Exceptions",
                "El legendario equipo de enanos que nunca captura sus propios errores.",
                "Mr. Abandonado", TeamCategory.QUARTET);
        Team runtimeRebels = saveTeam("Runtime Rebels",
                "Equipo emergente especializado en fallar rapido y fallar fuerte.",
                "Sebastian Zapata", TeamCategory.TRIO);

        addMember(fiveExceptions, nullPointer);
        addMember(fiveExceptions, stackOverflow);
        addMember(fiveExceptions, littleLambda);
        addMember(fiveExceptions, captainCache);
        addMember(runtimeRebels, tinyDocker);

        // ---------- Carreras en distintos estados ----------
        saveRace(organizer, "Clasico de Apertura EIA",
                "Carrera inaugural de la temporada. Aun en planeacion.",
                LocalDate.now().plusDays(30), LocalTime.of(9, 0), 1000, 6,
                RaceType.MIXED, RaceStatus.DRAFT, LocalDate.now().plusDays(25));

        Race openRace = saveRace(organizer, "Gran Premio Alto de Las Palmas",
                "Un kilometro de gloria entre camellos y enanos. Inscripciones abiertas.",
                LocalDate.now().plusDays(15), LocalTime.of(10, 30), 1000, 8,
                RaceType.MIXED, RaceStatus.OPEN_FOR_REGISTRATION, LocalDate.now().plusDays(10));

        Race completedRace = saveRace(organizer, "Copa Retro Zuniga 1979",
                "Reconstruccion historica de la primera carrera. Ya finalizada.",
                LocalDate.now().minusDays(20), LocalTime.of(8, 0), 1000, 4,
                RaceType.MIXED, RaceStatus.COMPLETED, LocalDate.now().minusDays(25));

        // ---------- Inscripciones ----------
        // Carrera abierta: una PENDING y una APPROVED
        saveRegistration(openRace, byteCamel, null, admin, RegistrationStatus.APPROVED, 1);
        saveRegistration(openRace, null, fiveExceptions, organizer, RegistrationStatus.PENDING, null);

        // Carrera completada: participantes aprobados
        saveRegistration(completedRace, kernelPanic, null, organizer, RegistrationStatus.APPROVED, 1);
        saveRegistration(completedRace, nullPointer, null, organizer, RegistrationStatus.APPROVED, 2);
        saveRegistration(completedRace, tinyDocker, null, organizer, RegistrationStatus.APPROVED, 3);

        // ---------- Resultados oficiales de la carrera completada ----------
        saveResult(completedRace, kernelPanic, organizer, ResultStatus.FINISHED,
                1, 1, 214_500, "Ganador indiscutible");
        saveResult(completedRace, nullPointer, organizer, ResultStatus.FINISHED,
                2, 2, 268_900, "Segundo lugar tras una remontada heroica");
        saveResult(completedRace, tinyDocker, organizer, ResultStatus.DID_NOT_FINISH,
                3, null, null, "Se detuvo a mitad de carrera: funcionaba en su maquina");

        // Estadisticas consistentes con los resultados anteriores
        applyStats(kernelPanic, 1, 0, 1);
        applyStats(nullPointer, 0, 1, 1);

        log.info("Datos semilla cargados: {} competidores, {} equipos, {} carreras, {} resultados.",
                playerRepository.count(), teamRepository.count(),
                raceRepository.count(), standingResultRepository.count());
    }

    // ------------------------------------------------------------------ helpers

    private User saveUser(String keycloakId, String username, String email) {
        return userRepository.save(User.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .build());
    }

    private Player savePlayer(String name, String nickname, PlayerType type,
                              LocalDate birthDate, String height, String weight, String placeOfBirth) {
        return playerRepository.save(Player.builder()
                .name(name)
                .nickname(nickname)
                .playerType(type)
                .birthDate(birthDate)
                .height(new BigDecimal(height))
                .weight(new BigDecimal(weight))
                .placeOfBirth(placeOfBirth)
                .actualState(PlayerState.ACTIVE)
                .registerDate(LocalDate.now().minusMonths(2))
                .victories(0)
                .defeats(0)
                .racesCompleted(0)
                .build());
    }

    private Team saveTeam(String teamName, String description, String coach, TeamCategory category) {
        return teamRepository.save(Team.builder()
                .teamName(teamName)
                .description(description)
                .responsableCoach(coach)
                .category(category)
                .status(TeamStatus.ACTIVE)
                .creationDate(LocalDate.now().minusMonths(1))
                .victories(0)
                .defeats(0)
                .build());
    }

    private void addMember(Team team, Player player) {
        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .player(player)
                .joinDate(LocalDate.now().minusMonths(1))
                .status(TeamMemberStatus.ACTIVE)
                .build());
    }

    private Race saveRace(User organizer, String raceName, String description,
                          LocalDate date, LocalTime hour, int distanceMeters, int maxPlayers,
                          RaceType type, RaceStatus status, LocalDate deadline) {
        return raceRepository.save(Race.builder()
                .organizer(organizer)
                .raceName(raceName)
                .description(description)
                .programationDate(date)
                .programationHour(hour)
                .ubicationStart("Campus EIA Zuniga")
                .ubicationFinish("Campus EIA Alto de Las Palmas")
                .distanceMeters(distanceMeters)
                .maxPlayers(maxPlayers)
                .raceType(type)
                .raceStatus(status)
                .registrationDeadline(deadline)
                .build());
    }

    private void saveRegistration(Race race, Player player, Team team, User registeringUser,
                                  RegistrationStatus status, Integer startPosition) {
        registerPlayerRepository.save(RegisterPlayer.builder()
                .race(race)
                .player(player)
                .team(team)
                .registeringUser(registeringUser)
                .status(status)
                .startPosition(startPosition)
                .assignedLane(startPosition)
                .build());
    }

    private void saveResult(Race race, Player player, User user, ResultStatus status,
                            Integer startPosition, Integer endPosition, Integer totalTimeMs, String notes) {
        standingResultRepository.save(StandingResult.builder()
                .race(race)
                .player(player)
                .user(user)
                .startPosition(startPosition)
                .endPosition(endPosition)
                .statusResult(status)
                .totalTimeMs(totalTimeMs)
                .penalizationTimeMs(0)
                .completedType(status == ResultStatus.FINISHED ? "OFFICIAL" : null)
                .notes(notes)
                .build());
    }

    private void applyStats(Player player, int victories, int defeats, int racesCompleted) {
        player.setVictories(victories);
        player.setDefeats(defeats);
        player.setRacesCompleted(racesCompleted);
        playerRepository.save(player);
    }
}