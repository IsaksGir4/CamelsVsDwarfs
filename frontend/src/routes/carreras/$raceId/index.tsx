import { useMemo, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Check, Flag, Plus, Trophy, X } from "lucide-react";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { StatusBadge } from "@/components/StatusBadge";
import { ErrorState, LoadingState } from "@/components/states";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth";
import { errorMessage } from "@/lib/api";
import {
  raceQuery,
  registrationsQuery,
  resultsQuery,
  competitorsQuery,
  teamsQuery,
  updateRaceStatus,
  createRegistration,
  approveRegistration,
  rejectRegistration,
  createResult,
  formatTime,
  raceDate,
  RACE_TRANSITIONS,
  RACE_STATUS_LABEL,
  RACE_TYPE_LABEL,
  RESULT_STATUSES,
  type Race,
  type Registration,
  type RaceResult,
} from "@/lib/league-api";

export const Route = createFileRoute("/carreras/$raceId/")({
  component: RaceDetailPage,
  errorComponent: ({ error }) => <div role="alert">{error instanceof Error ? error.message : "Error inesperado"}</div>,
  notFoundComponent: () => <div>Carrera no encontrada.</div>,
});

const FLOW = [
  "DRAFT",
  "OPEN_FOR_REGISTRATION",
  "CLOSED_FOR_REGISTRATION",
  "IN_PROGRESS",
  "COMPLETED",
];

const dateFmt = new Intl.DateTimeFormat("es", {
  day: "2-digit",
  month: "long",
  year: "numeric",
});

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium">{value}</dd>
    </div>
  );
}

function RaceDetailPage() {
  return (
    <RequireAuth>
      <AppLayout>
        <RaceDetailContent />
      </AppLayout>
    </RequireAuth>
  );
}

function RaceDetailContent() {
  const { raceId } = Route.useParams();
  const race = useQuery(raceQuery(raceId));
  const registrations = useQuery(registrationsQuery(raceId));
  const results = useQuery(resultsQuery(raceId));
  const { hasPermission } = useAuth();

  if (race.isPending) return <LoadingState rows={5} />;
  if (race.isError) {
    return (
      <ErrorState message={errorMessage(race.error)} onRetry={() => void race.refetch()} />
    );
  }

  const r = race.data;
  const d = raceDate(r);
  const approved = (registrations.data ?? []).filter((x) => x.status === "APPROVED");
  const active = (registrations.data ?? []).filter(
    (x) => x.status === "PENDING" || x.status === "APPROVED",
  );
  const showResults = r.status === "IN_PROGRESS" || r.status === "COMPLETED";

  return (
    <>
      <PageHeader
        title={r.name}
        description={r.description ?? "Detalle de la prueba."}
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to="/carreras">Volver</Link>
            </Button>
            {hasPermission("races:write") && (
              <Button asChild>
                <Link to="/carreras/$raceId/editar" params={{ raceId }}>Editar</Link>
              </Button>
            )}
          </>
        }
      />

      <div className="space-y-6">
        <StatusFlow race={r} />

        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Datos de la carrera</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="grid gap-5 sm:grid-cols-2">
                <Field label="Fecha" value={d ? dateFmt.format(d) : "—"} />
                <Field label="Hora" value={r.time?.slice(0, 5) ?? "—"} />
                <Field label="Salida" value={r.start} />
                <Field label="Meta" value={r.finish} />
                <Field label="Distancia" value={`${r.distanceMeters} m`} />
                <Field label="Tipo" value={RACE_TYPE_LABEL[r.type] ?? r.type} />
                <Field label="Cupo máximo" value={String(r.maxPlayers)} />
                <Field label="Cierre de inscripciones" value={r.registrationDeadline} />
                <Field label="Organizador" value={r.organizer} />
                <div>
                  <dt className="text-xs uppercase tracking-wide text-muted-foreground">Estado</dt>
                  <dd className="mt-1">
                    <StatusBadge status={r.status} />
                  </dd>
                </div>
              </dl>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Participación</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <p className="font-display text-4xl tabular-nums">
                {active.length}
                <span className="text-xl text-muted-foreground"> / {r.maxPlayers}</span>
              </p>
              <p className="text-sm text-muted-foreground">
                {approved.length} inscripcion{approved.length === 1 ? "" : "es"} aprobada
                {approved.length === 1 ? "" : "s"}. Un equipo ocupa una plaza.
              </p>
              {approved.length < 2 && r.status === "CLOSED_FOR_REGISTRATION" && (
                <p className="text-sm text-destructive">
                  Se necesitan al menos 2 participantes aprobados para iniciar la carrera.
                </p>
              )}
            </CardContent>
          </Card>
        </div>

        <RegistrationsCard raceId={raceId} race={r} query={registrations} />

        {showResults && !registrations.isPending && (
          <ResultsCard raceId={raceId} race={r} approved={approved} query={results} />
        )}
      </div>
    </>
  );
}

// ---------------------------------------------------------------- estados

function StatusFlow({ race }: { race: Race }) {
  const queryClient = useQueryClient();
  const { hasPermission } = useAuth();
  const [busy, setBusy] = useState(false);
  const [target, setTarget] = useState<string | null>(null);

  const allowed = RACE_TRANSITIONS[race.status] ?? [];
  const canWrite = hasPermission("races:write");
  const currentIndex = FLOW.indexOf(race.status);

  async function change(next: string) {
    setBusy(true);
    try {
      await updateRaceStatus(race.id, next);
      await queryClient.invalidateQueries({ queryKey: ["races"] });
      toast.success(`La carrera pasó a ${RACE_STATUS_LABEL[next] ?? next}.`);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
      setTarget(null);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Progreso de la carrera</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <ol className="flex flex-wrap items-center gap-x-2 gap-y-3">
          {FLOW.map((s, i) => {
            const done = currentIndex > i;
            const current = race.status === s;
            return (
              <li key={s} className="flex items-center gap-2">
                <span
                  className={cn(
                    "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium",
                    current && "border-transparent bg-primary text-primary-foreground",
                    done && "border-transparent bg-muted text-muted-foreground",
                    !current && !done && "text-muted-foreground",
                  )}
                >
                  {done && <Check className="size-3" aria-hidden />}
                  {RACE_STATUS_LABEL[s]}
                </span>
                {i < FLOW.length - 1 && (
                  <span className="h-px w-4 bg-border" aria-hidden />
                )}
              </li>
            );
          })}
          {race.status === "CANCELLED" && (
            <li>
              <StatusBadge status="CANCELLED" />
            </li>
          )}
        </ol>

        {canWrite && allowed.length > 0 && (
          <div className="flex flex-wrap gap-2 border-t pt-4">
            {allowed.map((s) => (
              <Button
                key={s}
                size="sm"
                variant={s === "CANCELLED" ? "outline" : "default"}
                disabled={busy}
                onClick={() => setTarget(s)}
              >
                {s === "CANCELLED" ? "Cancelar carrera" : `Pasar a ${RACE_STATUS_LABEL[s]}`}
              </Button>
            ))}
          </div>
        )}

        {canWrite && allowed.length === 0 && (
          <p className="border-t pt-4 text-sm text-muted-foreground">
            Esta carrera está en un estado final y no admite más cambios.
          </p>
        )}

        <AlertDialog open={target !== null} onOpenChange={(o) => !o && setTarget(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>
                {target === "CANCELLED" ? "¿Cancelar la carrera?" : "¿Cambiar el estado?"}
              </AlertDialogTitle>
              <AlertDialogDescription>
                {target === "CANCELLED"
                  ? "Una carrera cancelada no admite inscripciones ni vuelve a un estado anterior."
                  : `La carrera pasará a "${target ? RACE_STATUS_LABEL[target] : ""}". Algunas transiciones no se pueden deshacer.`}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={busy}>Volver</AlertDialogCancel>
              <AlertDialogAction
                disabled={busy}
                onClick={(e) => {
                  e.preventDefault();
                  if (target) void change(target);
                }}
              >
                {busy ? "Aplicando…" : "Confirmar"}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </CardContent>
    </Card>
  );
}

// ---------------------------------------------------------------- inscripciones

function RegistrationsCard({
  raceId,
  race,
  query,
}: {
  raceId: string;
  race: Race;
  query: ReturnType<typeof useQuery<Registration[]>>;
}) {
  const { hasPermission } = useAuth();
  const canManage = hasPermission("registrations:manage");

  return (
    <Card>
      <CardHeader>
        <CardTitle>Inscripciones</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {query.isPending ? (
          <LoadingState rows={3} />
        ) : query.isError ? (
          <ErrorState message={errorMessage(query.error)} onRetry={() => void query.refetch()} />
        ) : (query.data ?? []).length === 0 ? (
          <p className="text-sm text-muted-foreground">
            Todavía no hay inscripciones en esta carrera.
          </p>
        ) : (
          <div className="overflow-x-auto rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Participante</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead className="text-right">Salida</TableHead>
                  <TableHead className="text-right">Carril</TableHead>
                  {canManage && <TableHead className="w-40 text-right">Acciones</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {(query.data ?? []).map((reg) => (
                  <TableRow key={reg.idRegister}>
                    <TableCell className="font-medium">
                      {reg.participantType === "PLAYER" ? reg.playerNickname : reg.teamName}
                      {reg.validationNotes && (
                        <span className="block text-xs text-muted-foreground">
                          {reg.validationNotes}
                        </span>
                      )}
                    </TableCell>
                    <TableCell>
                      {reg.participantType === "PLAYER" ? "Competidor" : "Equipo"}
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={reg.status} />
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {reg.startPosition ?? "—"}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {reg.assignedLane ?? "—"}
                    </TableCell>
                    {canManage && (
                      <TableCell className="text-right">
                        {reg.status === "PENDING" && <RegistrationActions registration={reg} />}
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}

        {canManage && race.status === "OPEN_FOR_REGISTRATION" && (
          <AddRegistration raceId={raceId} race={race} existing={query.data ?? []} />
        )}
      </CardContent>
    </Card>
  );
}

function RegistrationActions({ registration }: { registration: Registration }) {
  const queryClient = useQueryClient();
  const [busy, setBusy] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["races"] });

  async function approve() {
    setBusy(true);
    try {
      await approveRegistration(registration.idRegister);
      await refresh();
      toast.success("Inscripción aprobada.");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function reject() {
    if (!reason.trim()) {
      toast.error("El motivo del rechazo es obligatorio.");
      return;
    }
    setBusy(true);
    try {
      await rejectRegistration(registration.idRegister, reason.trim());
      await refresh();
      toast.success("Inscripción rechazada.");
      setRejecting(false);
      setReason("");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="flex justify-end gap-1">
        <Button size="sm" variant="ghost" disabled={busy} onClick={() => void approve()}>
          <Check className="size-4" aria-hidden />
          Aprobar
        </Button>
        <Button size="sm" variant="ghost" disabled={busy} onClick={() => setRejecting(true)}>
          <X className="size-4" aria-hidden />
          Rechazar
        </Button>
      </div>

      <AlertDialog open={rejecting} onOpenChange={setRejecting}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Rechazar inscripción</AlertDialogTitle>
            <AlertDialogDescription>
              Indica el motivo del rechazo. Quedará registrado en la inscripción.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="space-y-2">
            <Label htmlFor="reason">Motivo</Label>
            <Input
              id="reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Ej.: el competidor no cumple los requisitos de la categoría"
            />
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={busy}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              disabled={busy}
              onClick={(e) => {
                e.preventDefault();
                void reject();
              }}
            >
              {busy ? "Rechazando…" : "Rechazar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}

function AddRegistration({
  raceId,
  race,
  existing,
}: {
  raceId: string;
  race: Race;
  existing: Registration[];
}) {
  const queryClient = useQueryClient();
  const competitors = useQuery(competitorsQuery());
  const teams = useQuery(teamsQuery());
  const [kind, setKind] = useState<"PLAYER" | "TEAM">(
    race.type === "TEAM" ? "TEAM" : "PLAYER",
  );
  const [selected, setSelected] = useState("");
  const [busy, setBusy] = useState(false);

  const takenPlayers = new Set(
    existing.filter((r) => r.playerId && r.status !== "REJECTED").map((r) => r.playerId!),
  );
  const takenTeams = new Set(
    existing.filter((r) => r.teamId && r.status !== "REJECTED").map((r) => r.teamId!),
  );

  const options = useMemo(() => {
    if (kind === "PLAYER") {
      return (competitors.data ?? [])
        .filter((c) => c.status === "ACTIVE" && !takenPlayers.has(c.id))
        .map((c) => ({ id: c.id, label: `${c.name} (${c.nickname})` }));
    }
    return (teams.data ?? [])
      .filter((t) => t.status === "ACTIVE" && !takenTeams.has(t.id))
      .map((t) => ({ id: t.id, label: t.name }));
  }, [kind, competitors.data, teams.data, existing]);

  const allowPlayer = race.type !== "TEAM";
  const allowTeam = race.type !== "INDIVIDUAL";

  async function submit() {
    if (!selected) return;
    setBusy(true);
    try {
      await createRegistration(
        raceId,
        kind === "PLAYER" ? { playerId: selected } : { teamId: selected },
      );
      await queryClient.invalidateQueries({ queryKey: ["races"] });
      toast.success("Inscripción creada en estado pendiente.");
      setSelected("");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="rounded-lg border border-dashed p-4">
      <h3 className="text-sm font-semibold">Inscribir participante</h3>
      <p className="mt-1 text-xs text-muted-foreground">
        {race.type === "INDIVIDUAL"
          ? "Esta carrera es individual: solo admite competidores."
          : race.type === "TEAM"
            ? "Esta carrera es por equipos: solo admite equipos."
            : "Esta carrera es mixta: admite competidores y equipos."}
      </p>

      <div className="mt-3 flex flex-wrap items-end gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="kind">Tipo</Label>
          <Select
            value={kind}
            onValueChange={(v) => {
              setKind(v as "PLAYER" | "TEAM");
              setSelected("");
            }}
          >
            <SelectTrigger id="kind" className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {allowPlayer && <SelectItem value="PLAYER">Competidor</SelectItem>}
              {allowTeam && <SelectItem value="TEAM">Equipo</SelectItem>}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="participant">Participante</Label>
          <Select value={selected} onValueChange={setSelected}>
            <SelectTrigger id="participant" className="w-72">
              <SelectValue placeholder="Selecciona…" />
            </SelectTrigger>
            <SelectContent>
              {options.length === 0 ? (
                <SelectItem value="none" disabled>
                  No hay participantes disponibles
                </SelectItem>
              ) : (
                options.map((o) => (
                  <SelectItem key={o.id} value={o.id}>
                    {o.label}
                  </SelectItem>
                ))
              )}
            </SelectContent>
          </Select>
        </div>

        <Button disabled={!selected || busy} onClick={() => void submit()}>
          <Plus className="size-4" aria-hidden />
          {busy ? "Inscribiendo…" : "Inscribir"}
        </Button>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------- resultados

function ResultsCard({
  raceId,
  race,
  approved,
  query,
}: {
  raceId: string;
  race: Race;
  approved: Registration[];
  query: ReturnType<typeof useQuery<RaceResult[]>>;
}) {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission("results:write");
  const withResult = new Set(
    (query.data ?? []).map((r) => r.playerId ?? r.teamId).filter(Boolean),
  );
  const pending = approved.filter(
    (a) => !withResult.has(a.playerId ?? a.teamId ?? ""),
  );
  

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Flag className="size-4 text-camel" aria-hidden />
          Resultados
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {query.isPending ? (
          <LoadingState rows={3} />
        ) : query.isError ? (
          <ErrorState message={errorMessage(query.error)} onRetry={() => void query.refetch()} />
        ) : (query.data ?? []).length === 0 ? (
          <p className="text-sm text-muted-foreground">
            Todavía no se han registrado resultados oficiales.
          </p>
        ) : (
          <div className="overflow-x-auto rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-16">Pos.</TableHead>
                  <TableHead>Participante</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead className="text-right">Tiempo</TableHead>
                  <TableHead className="text-right">Penalización</TableHead>
                  <TableHead>Notas</TableHead>
                </TableRow>
              </TableHeader>

              <TableBody>
                {[...(query.data ?? [])]
                  .sort((a, b) => (a.endPosition ?? 99) - (b.endPosition ?? 99))
                  .map((res) => (
                    <TableRow key={res.idStandingResult}>
                      <TableCell>
                        {res.endPosition === 1 ? (
                          <span className="inline-flex items-center gap-1.5 font-display text-lg">
                            <Trophy className="size-4 text-amber-500" aria-hidden />1
                          </span>
                        ) : (
                          <span className="font-display text-lg text-muted-foreground">
                            {res.endPosition ?? "—"}
                          </span>
                        )}
                      </TableCell>
                      <TableCell className="font-medium">
                        {res.playerNickname ?? res.teamName ?? "—"}
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={res.statusResult} />
                      </TableCell>
                      <TableCell className="text-right tabular-nums">
                        {formatTime(res.totalTimeMs)}
                      </TableCell>
                      <TableCell className="text-right tabular-nums">
                        {res.penalizationTimeMs ? `+${formatTime(res.penalizationTimeMs)}` : "—"}
                      </TableCell>
                      <TableCell className="max-w-56 truncate text-sm text-muted-foreground">
                        {res.notes ?? "—"}
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </div>
        )}
        {canWrite && race.status === "IN_PROGRESS" && (
          approved.length === 0 ? (
            <p className="rounded-lg border border-dashed p-4 text-sm text-muted-foreground">
              No hay participantes aprobados en esta carrera, así que no se pueden registrar resultados.
            </p>
          ) : pending.length > 0 ? (
            <AddResult raceId={raceId} pending={pending} existing={query.data ?? []} />
          ) : (
            <p className="rounded-lg border border-dashed p-4 text-sm text-muted-foreground">
              Todos los participantes aprobados ya tienen resultado. Ya puedes marcar la carrera como
              finalizada.
            </p>
          )
        )}
      </CardContent>
    </Card>
  );
}





function AddResult({
  raceId,
  pending,
  existing,
}: {
  raceId: string;
  pending: Registration[];
  existing: RaceResult[];
}) {
  const queryClient = useQueryClient();

  // Posiciones ya ocupadas por finalistas: se sugiere la primera libre para
  // evitar el 409 del backend por posicion duplicada.
  const takenPositions = new Set(
    existing
      .filter((r) => r.statusResult === "FINISHED" && r.endPosition != null)
      .map((r) => r.endPosition as number),
  );
  const nextFreePosition = (() => {
    let p = 1;
    while (takenPositions.has(p)) p++;
    return p;
  })();

  const [participant, setParticipant] = useState("");
  const [status, setStatus] = useState("FINISHED");
  const [timeMs, setTimeMs] = useState("");
  const [position, setPosition] = useState(String(nextFreePosition));
  const [penalty, setPenalty] = useState("0");
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);

  const needsTimeAndPosition = status === "FINISHED";
  const selected = pending.find((p) => (p.playerId ?? p.teamId) === participant);

  async function submit() {
    if (!selected) return;
    setBusy(true);
    try {
      await createResult(raceId, {
        ...(selected.playerId
          ? { playerId: selected.playerId }
          : { teamId: selected.teamId! }),
        statusResult: status,
        totalTimeMs: needsTimeAndPosition ? Number(timeMs) : null,
        endPosition: needsTimeAndPosition ? Number(position) : null,
        penalizationTimeMs: Number(penalty) || 0,
        notes: notes.trim() || undefined,
      });
      await queryClient.invalidateQueries({ queryKey: ["races"] });
      toast.success("Resultado registrado.");
      setParticipant("");
      setTimeMs("");
      setPosition("");
      setNotes("");
      setStatus("FINISHED");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="rounded-lg border border-dashed p-4">
      <h3 className="text-sm font-semibold">Registrar resultado</h3>
      <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <div className="space-y-1.5">
          <Label htmlFor="participant-result">Participante</Label>
          <Select value={participant} onValueChange={setParticipant}>
            <SelectTrigger id="participant-result">
              <SelectValue placeholder="Selecciona…" />
            </SelectTrigger>
            <SelectContent>
              {pending.map((p) => (
                <SelectItem key={p.idRegister} value={p.playerId ?? p.teamId ?? ""}>
                  {p.playerNickname ?? p.teamName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="status-result">Resultado</Label>
          <Select value={status} onValueChange={setStatus}>
            <SelectTrigger id="status-result">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {RESULT_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  <StatusBadge status={s} />
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="position">Posición final</Label>
          <Input
            id="position"
            type="number"
            value={position}
            disabled={!needsTimeAndPosition}
            onChange={(e) => setPosition(e.target.value)}
            placeholder={needsTimeAndPosition ? "1" : "No aplica"}
          />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="time">Tiempo total (ms)</Label>
          <Input
            id="time"
            type="number"
            value={timeMs}
            disabled={!needsTimeAndPosition}
            onChange={(e) => setTimeMs(e.target.value)}
            placeholder={needsTimeAndPosition ? "214500" : "No aplica"}
          />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="penalty">Penalización (ms)</Label>
          <Input
            id="penalty"
            type="number"
            min={0}
            value={penalty}
            onChange={(e) => setPenalty(e.target.value)}
          />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="notes">Notas</Label>
          <Input
            id="notes"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Opcional"
          />
        </div>
      </div>

      {needsTimeAndPosition && !takenPositions.has(1) && Number(position) !== 1 && (
        <p className="mt-3 text-sm text-amber-600">
          Todavía nadie ocupa la posición 1. Una carrera sin ganador quedará incompleta.
        </p>
      )}

      <Button
        className="mt-4"
        disabled={
          busy ||
          !participant ||
          (needsTimeAndPosition && (Number(timeMs) <= 0 || Number(position) <= 0))
        }
        onClick={() => void submit()}
      >
        {busy ? "Guardando…" : "Registrar resultado"}
      </Button>
    </div>
  );
}