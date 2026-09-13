import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { z } from "zod";
import { useMemo } from "react";
import { Plus, Search } from "lucide-react";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { StatusBadge } from "@/components/StatusBadge";
import { EmptyState, ErrorState, LoadingState } from "@/components/states";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
import { useAuth } from "@/lib/auth";
import {
  teamsQuery,
  teamCategoryLabel,
  teamMaxMembers,
  teamMemberCount,
  TEAM_STATUSES,
} from "@/lib/league-api";
import { errorMessage } from "@/lib/api";

const teamSearchSchema = z.object({
  q: z.string().catch("").default(""),
  estado: z.string().catch("all").default("all"),
});

export const Route = createFileRoute("/equipos/")({
  validateSearch: teamSearchSchema,
  component: TeamsPage,
  errorComponent: ({ error }) => <div role="alert">{error.message}</div>,
});

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Activo",
  INACTIVE: "Inactivo",
  DISBANDED: "Disuelto",
};

function TeamsPage() {
  return (
    <RequireAuth>
      <AppLayout>
        <TeamsContent />
      </AppLayout>
    </RequireAuth>
  );
}

function TeamsContent() {
  const { data, isPending, isError, error, refetch } = useQuery(teamsQuery());
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/equipos/" });
  const { hasPermission } = useAuth();

  const estado = (TEAM_STATUSES as readonly string[]).includes(search.estado)
    ? search.estado
    : "all";

  const setSearch = (patch: Partial<typeof search>) =>
    navigate({ search: (prev) => ({ ...prev, ...patch }) });

  const filtered = useMemo(() => {
    if (!data) return [];
    const q = search.q.trim().toLowerCase();
    return data.filter((t) => {
      if (estado !== "all" && t.status !== estado) return false;
      if (q && !t.name.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [data, search.q, estado]);

  const open = (id: string) => navigate({ to: "/equipos/$teamId", params: { teamId: id } });

  return (
    <>
      <PageHeader
        title="Equipos"
        description="Escuderías que compiten en la temporada."
        actions={
          hasPermission("teams:write") ? (
            <Button onClick={() => navigate({ to: "/equipos/nuevo" })}>
              <Plus className="size-4" aria-hidden />
              Nuevo equipo
            </Button>
          ) : undefined
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="relative min-w-56 flex-1 sm:max-w-xs">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            aria-hidden
          />
          <Input
            type="search"
            value={search.q}
            onChange={(e) => setSearch({ q: e.target.value })}
            placeholder="Buscar por nombre…"
            aria-label="Buscar equipo por nombre"
            className="pl-9"
          />
        </div>
        <Select value={estado} onValueChange={(v) => setSearch({ estado: v })}>
          <SelectTrigger className="w-48" aria-label="Filtrar por estado">
            <SelectValue placeholder="Estado" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos los estados</SelectItem>
            {TEAM_STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {STATUS_LABEL[s]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isPending ? (
        <LoadingState rows={6} />
      ) : isError ? (
        <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
      ) : filtered.length === 0 ? (
        <EmptyState
          title="Sin equipos"
          description={
            search.q || estado !== "all"
              ? "Ningún equipo coincide con los filtros aplicados. Prueba a ajustarlos."
              : "Todavía no hay equipos registrados en la liga."
          }
        />
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nombre</TableHead>
                  <TableHead>Categoría</TableHead>
                  <TableHead>Entrenador</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead className="text-right">Miembros</TableHead>
                  <TableHead className="text-right">V / D</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((t) => {
                  const max = teamMaxMembers(t);
                  return (
                    <TableRow
                      key={t.id}
                      tabIndex={0}
                      role="link"
                      className="cursor-pointer"
                      onClick={() => open(t.id)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          open(t.id);
                        }
                      }}
                    >
                      <TableCell className="font-medium">{t.name}</TableCell>
                      <TableCell>{teamCategoryLabel(t)}</TableCell>
                      <TableCell className="text-muted-foreground">{t.coach ?? "—"}</TableCell>
                      <TableCell>
                        <StatusBadge status={t.status} />
                      </TableCell>
                      <TableCell className="text-right tabular-nums">
                        {teamMemberCount(t)}
                        {max != null ? ` / ${max}` : ""}
                      </TableCell>
                      <TableCell className="text-right tabular-nums">
                        {t.wins} / {t.losses}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
          <p className="mt-2 text-xs text-muted-foreground">
            {filtered.length} equipo{filtered.length === 1 ? "" : "s"}
          </p>
        </>
      )}
    </>
  );
}