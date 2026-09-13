import { useMemo, useState } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Search, Trash2, UserMinus, UserPlus } from "lucide-react";
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
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useAuth } from "@/lib/auth";
import { errorMessage } from "@/lib/api";
import {
  addTeamMember,
  competitorName,
  competitorNickname,
  competitorTypeLabel,
  competitorsQuery,
  deleteTeam,
  removeTeamMember,
  teamCategoryLabel,
  teamMaxMembers,
  teamMemberCount,
  teamMembersQuery,
  teamQuery,
  updateTeamStatus,
  TEAM_STATUSES,
  type Competitor,
  type Team,
} from "@/lib/league-api";

export const Route = createFileRoute("/equipos/$teamId/")({
  component: TeamDetailPage,
  errorComponent: ({ error }) => <div role="alert">{error instanceof Error ? error.message : "Error inesperado"}</div>,
  notFoundComponent: () => <div>Equipo no encontrado.</div>,
});

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium">{value}</dd>
    </div>
  );
}

function formatDate(raw?: string): string {
  if (!raw) return "—";
  const d = new Date(raw);
  return Number.isNaN(d.getTime())
    ? "—"
    : d.toLocaleDateString("es-ES", { day: "2-digit", month: "long", year: "numeric" });
}

function TeamDetailPage() {
  const { teamId } = Route.useParams();
  const { hasPermission } = useAuth();
  const canWrite = hasPermission("teams:write");
  const team = useQuery(teamQuery(teamId));
  const members = useQuery(teamMembersQuery(teamId));

  const isPending = team.isPending || members.isPending;
  const isError = team.isError || members.isError;

  const activeMembers = useMemo(
    () => (members.data ?? []).filter((m) => m.status === "ACTIVE"),
    [members.data],
  );

  const max = team.data ? teamMaxMembers(team.data) : null;
  const count = team.data ? teamMemberCount(team.data, activeMembers) : 0;
  const isFull = max != null && count >= max;

  return (
    <RequireAuth>
      <AppLayout>
        <PageHeader
          title={team.data?.name ?? "Detalle de equipo"}
          description="Datos del equipo y plantilla de miembros."
          actions={
            <>
              <Button variant="outline" asChild>
                <Link to="/equipos">Volver</Link>
              </Button>
              {canWrite && (
                <Button asChild>
                  <Link to="/equipos/$teamId/editar" params={{ teamId }}>
                    Editar
                  </Link>
                </Button>
              )}
            </>
          }
        />

        {isPending ? (
          <LoadingState rows={4} />
        ) : isError ? (
          <ErrorState
            message={errorMessage(team.error ?? members.error)}
            onRetry={() => {
              void team.refetch();
              void members.refetch();
            }}
          />
        ) : (
          <div className="space-y-6">
            <div className="grid gap-6 lg:grid-cols-3">
              <Card className="lg:col-span-2">
                <CardHeader>
                  <CardTitle>Datos del equipo</CardTitle>
                </CardHeader>
                <CardContent>
                  <dl className="grid gap-5 sm:grid-cols-2">
                    <Field label="Nombre" value={team.data.name} />
                    <Field label="Categoría" value={teamCategoryLabel(team.data)} />
                    <Field label="Entrenador" value={team.data.coach ?? "—"} />
                    <Field label="Fundación" value={formatDate(team.data.foundedAt)} />
                    <Field
                      label="Victorias / Derrotas"
                      value={`${team.data.wins} / ${team.data.losses}`}
                    />
                    <div>
                      <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                        Estado
                      </dt>
                      <dd className="mt-1">
                        <StatusBadge status={team.data.status} />
                      </dd>
                    </div>
                    <div className="sm:col-span-2">
                      <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                        Descripción
                      </dt>
                      <dd className="mt-0.5 text-sm">{team.data.description || "—"}</dd>
                    </div>
                  </dl>
                </CardContent>
              </Card>

              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Plantilla</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    <p className="font-display text-4xl tabular-nums">
                      {count}
                      <span className="text-xl text-muted-foreground">
                        {max != null ? ` / ${max}` : ""}
                      </span>
                    </p>
                    <p className="text-sm text-muted-foreground">
                      {max == null
                        ? "Miembros registrados en el equipo."
                        : isFull
                          ? `El equipo está completo: la categoría ${teamCategoryLabel(team.data)} admite un máximo de ${max} miembros.`
                          : `Quedan ${max - count} plaza${max - count === 1 ? "" : "s"} libres para la categoría ${teamCategoryLabel(team.data)}.`}
                    </p>
                  </CardContent>
                </Card>

                {canWrite && (
                  <Card>
                    <CardHeader>
                      <CardTitle>Estado</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <StatusChanger team={team.data} />
                      <DeleteTeamButton team={team.data} />
                    </CardContent>
                  </Card>
                )}
              </div>
            </div>

            <Card>
              <CardHeader>
                <CardTitle>Miembros activos</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {activeMembers.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    Este equipo todavía no tiene miembros activos. Necesita al menos uno para poder
                    inscribirse en una carrera.
                  </p>
                ) : (
                  <div className="overflow-x-auto rounded-lg border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Nombre</TableHead>
                          <TableHead>Apodo</TableHead>
                          <TableHead>Estado</TableHead>
                          {canWrite && <TableHead className="w-24 text-right">Acciones</TableHead>}
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {activeMembers.map((m) => (
                          <TableRow key={m.id}>
                            <TableCell className="font-medium">{competitorName(m)}</TableCell>
                            <TableCell>{competitorNickname(m)}</TableCell>
                            <TableCell>
                              <StatusBadge status={m.status} />
                            </TableCell>
                            {canWrite && (
                              <TableCell className="text-right">
                                <RemoveMemberButton teamId={teamId} member={m} />
                              </TableCell>
                            )}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                )}

                {canWrite && (
                  <AddMemberPanel
                    teamId={teamId}
                    isFull={isFull}
                    max={max}
                    currentIds={new Set(activeMembers.map((m) => m.id))}
                  />
                )}
              </CardContent>
            </Card>
          </div>
        )}
      </AppLayout>
    </RequireAuth>
  );
}

function StatusChanger({ team }: { team: Team }) {
  const queryClient = useQueryClient();
  const [saving, setSaving] = useState(false);

  async function change(next: string) {
    if (next === team.status) return;
    setSaving(true);
    try {
      await updateTeamStatus(team.id, next);
      await queryClient.invalidateQueries({ queryKey: ["teams"] });
      toast.success("Estado actualizado.");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-2">
      <Label htmlFor="team-status">Cambiar estado</Label>
      <Select value={team.status} onValueChange={(v) => void change(v)} disabled={saving}>
        <SelectTrigger id="team-status">
          <SelectValue placeholder="Selecciona un estado" />
        </SelectTrigger>
        <SelectContent>
          {TEAM_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              <StatusBadge status={s} />
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <p className="text-xs text-muted-foreground">
        Solo los equipos ACTIVE pueden inscribirse en carreras.
      </p>
    </div>
  );
}

function DeleteTeamButton({ team }: { team: Team }) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  async function confirm() {
    setBusy(true);
    try {
      await deleteTeam(team.id);
      await queryClient.invalidateQueries({ queryKey: ["teams"] });
      toast.success("Equipo eliminado.");
      await navigate({ to: "/equipos" });
    } catch (error) {
      toast.error(errorMessage(error));
      setOpen(false);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Button variant="outline" className="w-full gap-2" onClick={() => setOpen(true)}>
        <Trash2 className="size-4" aria-hidden />
        Eliminar equipo
      </Button>
      <AlertDialog open={open} onOpenChange={setOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Eliminar el equipo?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta acción no se puede deshacer. Un equipo con historial de carreras no se puede
              eliminar: en ese caso debe desactivarse.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={busy}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              disabled={busy}
              onClick={(e) => {
                e.preventDefault();
                void confirm();
              }}
            >
              {busy ? "Eliminando…" : "Eliminar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}

function RemoveMemberButton({ teamId, member }: { teamId: string; member: Competitor }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  async function confirm() {
    setBusy(true);
    try {
      await removeTeamMember(teamId, member.id);
      await queryClient.invalidateQueries({ queryKey: ["teams"] });
      toast.success(`${competitorName(member)} salió del equipo.`);
      setOpen(false);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => setOpen(true)}
        aria-label={`Quitar a ${competitorName(member)} del equipo`}
      >
        <UserMinus className="size-4" aria-hidden />
        Quitar
      </Button>
      <AlertDialog open={open} onOpenChange={setOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Quitar del equipo?</AlertDialogTitle>
            <AlertDialogDescription>
              {competitorName(member)} dejará de formar parte de la plantilla. Podrás volver a
              añadirlo más adelante si queda plaza.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={busy}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault();
                void confirm();
              }}
              disabled={busy}
            >
              {busy ? "Quitando…" : "Quitar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}

function AddMemberPanel({
  teamId,
  isFull,
  max,
  currentIds,
}: {
  teamId: string;
  isFull: boolean;
  max: number | null;
  currentIds: Set<string>;
}) {
  const queryClient = useQueryClient();
  const competitors = useQuery(competitorsQuery());
  const [query, setQuery] = useState("");
  const [busyId, setBusyId] = useState<string | null>(null);

  const matches = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    return (competitors.data ?? [])
      .filter((c) => !currentIds.has(c.id) && c.status === "ACTIVE")
      .filter(
        (c) =>
          competitorName(c).toLowerCase().includes(q) ||
          competitorNickname(c).toLowerCase().includes(q),
      )
      .slice(0, 8);
  }, [competitors.data, query, currentIds]);

  async function add(c: Competitor) {
    setBusyId(c.id);
    try {
      await addTeamMember(teamId, c.id);
      await queryClient.invalidateQueries({ queryKey: ["teams"] });
      toast.success(`${competitorName(c)} se unió al equipo.`);
      setQuery("");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="rounded-lg border border-dashed p-4">
      <h3 className="text-sm font-semibold">Agregar miembro</h3>
      {isFull ? (
        <p role="status" className="mt-2 text-sm text-muted-foreground">
          No se pueden agregar más miembros: el equipo ya alcanzó el máximo de {max} permitido por
          su categoría. Quita a alguien de la plantilla para liberar una plaza.
        </p>
      ) : (
        <>
          <p className="mt-1 text-xs text-muted-foreground">
            Solo aparecen competidores ACTIVE que no pertenezcan ya a este equipo.
          </p>
          <div className="relative mt-3 max-w-sm">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar competidor por nombre o apodo…"
              aria-label="Buscar competidor para agregar"
              className="pl-9"
            />
          </div>
          {competitors.isError ? (
            <p className="mt-3 text-sm text-destructive">{errorMessage(competitors.error)}</p>
          ) : query.trim() && matches.length === 0 && !competitors.isPending ? (
            <p className="mt-3 text-sm text-muted-foreground">
              Ningún competidor disponible coincide con la búsqueda.
            </p>
          ) : (
            <ul className="mt-3 space-y-1">
              {matches.map((c) => (
                <li
                  key={c.id}
                  className="flex items-center justify-between gap-3 rounded-md px-2 py-1.5 hover:bg-muted"
                >
                  <span className="text-sm">
                    {competitorName(c)}{" "}
                    <span className="text-muted-foreground">
                      · {competitorTypeLabel(c.type)}
                    </span>
                  </span>
                  <Button size="sm" disabled={busyId === c.id} onClick={() => void add(c)}>
                    <UserPlus className="size-4" aria-hidden />
                    {busyId === c.id ? "Agregando…" : "Agregar"}
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}