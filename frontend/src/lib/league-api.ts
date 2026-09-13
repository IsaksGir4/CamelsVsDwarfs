import { queryOptions } from "@tanstack/react-query";
import { api } from "./api";

// ---------------------------------------------------------------------------
// Capa de adaptacion: la API expone PlayerResponseDTO / TeamResponseDTO con
// nombres propios del backend. Aqui se normalizan a una forma estable para la
// UI, de modo que un cambio de contrato solo afecte a este archivo.
// ---------------------------------------------------------------------------

interface PlayerDTO {
  idPlayer: string;
  name: string;
  nickname: string;
  playerType: string;
  birthDate: string;
  height: number | null;
  weight: number | null;
  placeOfBirth: string;
  actualState: string;
  registerDate: string;
  victories: number;
  defeats: number;
  racesCompleted: number;
}

interface TeamDTO {
  idTeam: string;
  teamName: string;
  description: string;
  creationDate: string;
  status: string;
  responsableCoach: string | null;
  category: string;
  currentMemberCount: number;
  victories: number;
  defeats: number;
}

interface TeamMemberDTO {
  idTeamMember: string;
  playerId: string;
  playerName: string;
  playerNickname: string;
  joinDate: string;
  leaveDate: string | null;
  status: string;
}

interface RaceDTO {
  idRace: string;
  organizerId: string;
  organizerUsername: string;
  raceName: string;
  description: string | null;
  programationDate: string;
  programationHour: string;
  ubicationStart: string;
  ubicationFinish: string;
  distanceMeters: number;
  maxPlayers: number;
  raceType: string;
  raceStatus: string;
  registrationDeadline: string;
  creationDate: string;
  lastUpdate: string;
}

export interface Competitor {
  id: string;
  name: string;
  nickname: string;
  type: string;
  status: string;
  birthDate: string;
  height: number | null;
  weight: number | null;
  origin: string;
  registerDate: string;
  wins: number;
  losses: number;
  racesCompleted: number;
}

export interface Team {
  id: string;
  name: string;
  description: string;
  status: string;
  category: string;
  coach: string | null;
  foundedAt: string;
  memberCount: number;
  maxMembers: number;
  wins: number;
  losses: number;
}

export interface Race {
  id: string;
  name: string;
  description: string | null;
  date: string;
  time: string;
  start: string;
  finish: string;
  distanceMeters: number;
  maxPlayers: number;
  type: string;
  status: string;
  registrationDeadline: string;
  organizer: string;
}

export interface Registration {
  idRegister: string;
  raceId: string;
  raceName: string;
  participantType: "PLAYER" | "TEAM";
  playerId: string | null;
  playerNickname: string | null;
  teamId: string | null;
  teamName: string | null;
  registeredBy: string | null;
  registerDate: string;
  status: string;
  assignedLane: number | null;
  startPosition: number | null;
  validationNotes: string | null;
}

export interface RaceResult {
  idStandingResult: string;
  raceId: string;
  playerId: string | null;
  playerNickname: string | null;
  teamId: string | null;
  teamName: string | null;
  startPosition: number | null;
  endPosition: number | null;
  completedType: string | null;
  penalizationTimeMs: number;
  statusResult: string;
  totalTimeMs: number | null;
  notes: string | null;
  timestamp: string;
}

export interface PlayerStanding {
  playerId: string;
  name: string;
  nickname: string;
  points: number;
  victories: number;
  defeats: number;
  racesCompleted: number;
}

export interface TeamStanding {
  teamId: string;
  teamName: string;
  points: number;
  victories: number;
  defeats: number;
}

// ---------------------------------------------------------------------------
// Catalogos (deben coincidir con los enums del backend)
// ---------------------------------------------------------------------------

export const COMPETITOR_TYPES = ["DWARF", "CAMEL", "MEDIUM", "OTHER"] as const;
export const COMPETITOR_STATUSES = ["ACTIVE", "INJURED", "SUSPENDED", "RETIRED"] as const;
export const TEAM_STATUSES = ["ACTIVE", "INACTIVE", "DISBANDED"] as const;
export const TEAM_CATEGORIES = ["DUO", "TRIO", "QUARTET"] as const;
export const RACE_TYPES = ["INDIVIDUAL", "TEAM", "MIXED"] as const;
export const RACE_STATUSES = [
  "DRAFT",
  "OPEN_FOR_REGISTRATION",
  "CLOSED_FOR_REGISTRATION",
  "IN_PROGRESS",
  "COMPLETED",
  "CANCELLED",
] as const;
export const REGISTRATION_STATUSES = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"] as const;
export const RESULT_STATUSES = [
  "FINISHED",
  "DISQUALIFIED",
  "DID_NOT_FINISH",
  "DID_NOT_START",
] as const;

export const COMPETITOR_TYPE_LABEL: Record<string, string> = {
  DWARF: "Enano",
  CAMEL: "Camello",
  MEDIUM: "Mediano",
  OTHER: "Otro",
};

export const TEAM_CATEGORY_LABEL: Record<string, string> = {
  DUO: "Duo (2)",
  TRIO: "Trio (3)",
  QUARTET: "Cuarteto (4)",
};

export const RACE_TYPE_LABEL: Record<string, string> = {
  INDIVIDUAL: "Individual",
  TEAM: "Por equipos",
  MIXED: "Mixta",
};

export const RACE_STATUS_LABEL: Record<string, string> = {
  DRAFT: "Borrador",
  OPEN_FOR_REGISTRATION: "Inscripciones abiertas",
  CLOSED_FOR_REGISTRATION: "Inscripciones cerradas",
  IN_PROGRESS: "En curso",
  COMPLETED: "Finalizada",
  CANCELLED: "Cancelada",
};

export const TEAM_CATEGORY_MAX: Record<string, number> = { DUO: 2, TRIO: 3, QUARTET: 4 };

/** Transiciones permitidas, espejo de ALLOWED_TRANSITIONS en RaceService. */
export const RACE_TRANSITIONS: Record<string, string[]> = {
  DRAFT: ["OPEN_FOR_REGISTRATION", "CANCELLED"],
  OPEN_FOR_REGISTRATION: ["CLOSED_FOR_REGISTRATION", "CANCELLED"],
  CLOSED_FOR_REGISTRATION: ["IN_PROGRESS", "OPEN_FOR_REGISTRATION", "CANCELLED"],
  IN_PROGRESS: ["COMPLETED", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
};

export const UPCOMING_STATUSES = [
  "OPEN_FOR_REGISTRATION",
  "CLOSED_FOR_REGISTRATION",
  "IN_PROGRESS",
];

export const POINTS_TABLE = [
  { position: "1.º", points: 10 },
  { position: "2.º", points: 7 },
  { position: "3.º", points: 5 },
  { position: "4.º", points: 3 },
  { position: "5.º", points: 1 },
  { position: "No termina / descalificado", points: 0 },
];

// ---------------------------------------------------------------------------
// Mapeadores
// ---------------------------------------------------------------------------

function toCompetitor(d: PlayerDTO): Competitor {
  return {
    id: d.idPlayer,
    name: d.name,
    nickname: d.nickname,
    type: d.playerType,
    status: d.actualState,
    birthDate: d.birthDate,
    height: d.height,
    weight: d.weight,
    origin: d.placeOfBirth,
    registerDate: d.registerDate,
    wins: d.victories ?? 0,
    losses: d.defeats ?? 0,
    racesCompleted: d.racesCompleted ?? 0,
  };
}

function toTeam(d: TeamDTO): Team {
  return {
    id: d.idTeam,
    name: d.teamName,
    description: d.description,
    status: d.status,
    category: d.category,
    coach: d.responsableCoach,
    foundedAt: d.creationDate,
    memberCount: d.currentMemberCount ?? 0,
    maxMembers: TEAM_CATEGORY_MAX[d.category] ?? 0,
    wins: d.victories ?? 0,
    losses: d.defeats ?? 0,
  };
}

function toRace(d: RaceDTO): Race {
  return {
    id: d.idRace,
    name: d.raceName,
    description: d.description,
    date: d.programationDate,
    time: d.programationHour,
    start: d.ubicationStart,
    finish: d.ubicationFinish,
    distanceMeters: d.distanceMeters,
    maxPlayers: d.maxPlayers,
    type: d.raceType,
    status: d.raceStatus,
    registrationDeadline: d.registrationDeadline,
    organizer: d.organizerUsername,
  };
}

function toMember(d: TeamMemberDTO): Competitor {
  return {
    id: d.playerId,
    name: d.playerName,
    nickname: d.playerNickname,
    type: "",
    status: d.status,
    birthDate: "",
    height: null,
    weight: null,
    origin: "",
    registerDate: d.joinDate,
    wins: 0,
    losses: 0,
    racesCompleted: 0,
  };
}

/** Desempaqueta el objeto Page de Spring o una lista simple. */
async function getPage<T>(path: string): Promise<T[]> {
  const data = await api.get<T[] | { content?: T[] }>(path);
  if (Array.isArray(data)) return data;
  return data?.content ?? [];
}

// ---------------------------------------------------------------------------
// Helpers de presentacion (los usan las tablas)
// ---------------------------------------------------------------------------

export const competitorName = (c: Competitor) => c.name || "Sin nombre";
export const competitorNickname = (c: Competitor) => c.nickname || "—";
export const competitorWins = (c: Competitor) => c.wins;
export const competitorLosses = (c: Competitor) => c.losses;
export const competitorRacesCompleted = (c: Competitor) => c.racesCompleted;
export const competitorTypeLabel = (type?: string) =>
  type ? (COMPETITOR_TYPE_LABEL[type] ?? type) : "—";

export const teamCategory = (t: Team) => t.category;
export const teamCategoryLabel = (t: Team) =>
  t.category ? (TEAM_CATEGORY_LABEL[t.category] ?? t.category) : "—";
export const teamMaxMembers = (t: Team) => t.maxMembers || null;
export const teamMemberCount = (t: Team, members?: Competitor[]) =>
  members ? members.length : t.memberCount;

export function raceDate(r: Race): Date | null {
  if (!r.date) return null;
  const d = new Date(`${r.date}T${r.time ?? "00:00:00"}`);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function formatTime(ms: number | null | undefined): string {
  if (ms == null) return "—";
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  const millis = ms % 1000;
  return `${minutes}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
}

// ---------------------------------------------------------------------------
// Competidores
// ---------------------------------------------------------------------------

export interface CompetitorInput {
  name: string;
  nickname: string;
  playerType: string;
  birthDate: string;
  height: number;
  weight: number;
  placeOfBirth: string;
}

export const competitorsQuery = () =>
  queryOptions({
    queryKey: ["competitors"],
    queryFn: async () => (await getPage<PlayerDTO>("/players?size=200")).map(toCompetitor),
    retry: 1,
  });

export const competitorQuery = (id: string) =>
  queryOptions({
    queryKey: ["competitors", id],
    queryFn: async () => toCompetitor(await api.get<PlayerDTO>(`/players/${id}`)),
    retry: 1,
  });

export const createCompetitor = (input: CompetitorInput) =>
  api.post<PlayerDTO>("/players", input);

export const updateCompetitor = (id: string, input: CompetitorInput) =>
  api.put<PlayerDTO>(`/players/${id}`, input);

export const updateCompetitorStatus = (id: string, newState: string) =>
  api.patch<PlayerDTO>(`/players/${id}/status`, { newState });

export const deleteCompetitor = (id: string) => api.delete<void>(`/players/${id}`);

// ---------------------------------------------------------------------------
// Equipos
// ---------------------------------------------------------------------------

export interface TeamInput {
  teamName: string;
  description: string;
  responsableCoach: string;
  category: string;
}

export const teamsQuery = () =>
  queryOptions({
    queryKey: ["teams"],
    queryFn: async () => (await getPage<TeamDTO>("/teams?size=200")).map(toTeam),
    retry: 1,
  });

export const teamQuery = (id: string) =>
  queryOptions({
    queryKey: ["teams", id],
    queryFn: async () => toTeam(await api.get<TeamDTO>(`/teams/${id}`)),
    retry: 1,
  });

export const teamMembersQuery = (id: string) =>
  queryOptions({
    queryKey: ["teams", id, "members"],
    queryFn: async () => (await getPage<TeamMemberDTO>(`/teams/${id}/members`)).map(toMember),
    retry: 1,
  });

export const createTeam = (input: TeamInput) => api.post<TeamDTO>("/teams", input);

export const updateTeam = (id: string, input: TeamInput) =>
  api.put<TeamDTO>(`/teams/${id}`, input);

export const updateTeamStatus = (id: string, newState: string) =>
  api.patch<TeamDTO>(`/teams/${id}/status`, { newState });

export const deleteTeam = (id: string) => api.delete<void>(`/teams/${id}`);

export const addTeamMember = (teamId: string, competitorId: string) =>
  api.post<unknown>(`/teams/${teamId}/members/${competitorId}`);

export const removeTeamMember = (teamId: string, competitorId: string) =>
  api.delete<void>(`/teams/${teamId}/members/${competitorId}`);

// ---------------------------------------------------------------------------
// Carreras
// ---------------------------------------------------------------------------

export interface RaceInput {
  raceName: string;
  description: string;
  programationDate: string;
  programationHour: string;
  ubicationStart: string;
  ubicationFinish: string;
  distanceMeters: number;
  maxPlayers: number;
  raceType: string;
  registrationDeadline: string;
}

export const racesQuery = () =>
  queryOptions({
    queryKey: ["races"],
    queryFn: async () => (await getPage<RaceDTO>("/races?size=200")).map(toRace),
    retry: 1,
  });

export const raceQuery = (id: string) =>
  queryOptions({
    queryKey: ["races", id],
    queryFn: async () => toRace(await api.get<RaceDTO>(`/races/${id}`)),
    retry: 1,
  });

export const createRace = (input: RaceInput) => api.post<RaceDTO>("/races", input);

export const updateRace = (id: string, input: RaceInput) =>
  api.put<RaceDTO>(`/races/${id}`, input);

export const updateRaceStatus = (id: string, newStatus: string) =>
  api.patch<RaceDTO>(`/races/${id}/status`, { newStatus });

export const deleteRace = (id: string) => api.delete<void>(`/races/${id}`);

// ---------------------------------------------------------------------------
// Inscripciones
// ---------------------------------------------------------------------------

export const registrationsQuery = (raceId: string) =>
  queryOptions({
    queryKey: ["races", raceId, "registrations"],
    queryFn: () => api.get<Registration[]>(`/races/${raceId}/registrations`),
    retry: 1,
  });

export const createRegistration = (
  raceId: string,
  input: { playerId?: string; teamId?: string; startPosition?: number },
) => api.post<Registration>(`/races/${raceId}/registrations`, input);

export const approveRegistration = (id: string) =>
  api.patch<Registration>(`/registrations/${id}/approve`);

export const rejectRegistration = (id: string, reason: string) =>
  api.patch<Registration>(`/registrations/${id}/reject`, { reason });

export const cancelRegistration = (id: string) => api.delete<void>(`/registrations/${id}`);

// ---------------------------------------------------------------------------
// Resultados y clasificacion
// ---------------------------------------------------------------------------

export interface ResultInput {
  playerId?: string;
  teamId?: string;
  statusResult: string;
  totalTimeMs?: number | null;
  endPosition?: number | null;
  penalizationTimeMs?: number;
  completedType?: string;
  notes?: string;
}

export const resultsQuery = (raceId: string) =>
  queryOptions({
    queryKey: ["races", raceId, "results"],
    queryFn: () => api.get<RaceResult[]>(`/races/${raceId}/results`),
    retry: 1,
  });

export const createResult = (raceId: string, input: ResultInput) =>
  api.post<RaceResult>(`/races/${raceId}/results`, input);

export const updateResult = (id: string, input: Omit<ResultInput, "playerId" | "teamId">) =>
  api.put<RaceResult>(`/results/${id}`, input);

export const playerStandingsQuery = () =>
  queryOptions({
    queryKey: ["standings", "competitors"],
    queryFn: () => api.get<PlayerStanding[]>("/standings/competitors"),
    retry: 1,
  });

export const teamStandingsQuery = () =>
  queryOptions({
    queryKey: ["standings", "teams"],
    queryFn: () => api.get<TeamStanding[]>("/standings/teams"),
    retry: 1,
  });

// ---------------------------------------------------------------------------
// Auditoria y perfil
// ---------------------------------------------------------------------------

export interface AuditEntry {
  idAuditLog?: string;
  username?: string;
  action?: string;
  entityType?: string;
  entityId?: string;
  description?: string;
  oldValue?: string;
  newValue?: string;
  timestamp?: string;
}

export const auditQuery = (page = 0) =>
  queryOptions({
    queryKey: ["audit", page],
    queryFn: () => getPage<AuditEntry>(`/audit?page=${page}&size=20`),
    retry: 1,
  });

export const profileQuery = () =>
  queryOptions({
    queryKey: ["profile"],
    queryFn: () =>
      api.get<{ id: string; username: string; email: string; roles: string[] }>("/auth/profile"),
    retry: 1,
  });