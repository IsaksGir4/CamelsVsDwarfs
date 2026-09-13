import { useMemo } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { z } from "zod";
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
import { errorMessage } from "@/lib/api";
import {
  racesQuery,
  raceDate,
  RACE_STATUSES,
  RACE_STATUS_LABEL,
  RACE_TYPES,
  RACE_TYPE_LABEL,
} from "@/lib/league-api";

const raceSearchSchema = z.object({
  q: z.string().catch("").default(""),
  estado: z.string().catch("all").default("all"),
  tipo: z.string().catch("all").default("all"),
});

export const Route = createFileRoute("/carreras/")({
  validateSearch: raceSearchSchema,
  component: RacesPage,
  errorComponent: ({ error }) => <div role="alert">{error.message}</div>,
});

const dateFmt = new Intl.DateTimeFormat("es", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

function RacesPage() {
  return (
    <RequireAuth>
      <AppLayout>
        <RacesContent />
      </AppLayout>
    </RequireAuth>
  );
}

function RacesContent() {
  const { data, isPending, isError, error, refetch } = useQuery(racesQuery());
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/carreras/" });
  const { hasPermission } = useAuth();

  const estado = (RACE_STATUSES as readonly string[]).includes(search.estado)
    ? search.estado
    : "all";
  const tipo = (RACE_TYPES as readonly string[]).includes(search.tipo) ? search.tipo : "all";

  const setSearch = (patch: Partial<typeof search>) =>
    navigate({ search: (prev) => ({ ...prev, ...patch }) });

  const filtered = useMemo(() => {
    if (!data) return [];
    const q = search.q.trim().toLowerCase();
    return data
      .filter((r) => {
        if (estado !== "all" && r.status !== estado) return false;
        if (tipo !== "all" && r.type !== tipo) return false;
        if (q && !r.name.toLowerCase().includes(q)) return false;
        return true;
      })
      .sort((a, b) => (raceDate(b)?.getTime() ?? 0) - (raceDate(a)?.getTime() ?? 0));
  }, [data, search.q, estado, tipo]);

  const open = (id: string) => navigate({ to: "/carreras/$raceId", params: { raceId: id } });

  return (
    <>
      <PageHeader
        title="Carreras"
        description="Calendario y estado de cada prueba de la temporada."
        actions={
          hasPermission("races:write") ? (
            <Button onClick={() => navigate({ to: "/carreras/nueva" })}>
              <Plus className="size-4" aria-hidden />
              Nueva carrera
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
            aria-label="Buscar carrera por nombre"
            className="pl-9"
          />
        </div>
        <Select value={estado} onValueChange={(v) => setSearch({ estado: v })}>
          <SelectTrigger className="w-56" aria-label="Filtrar por estado">
            <SelectValue placeholder="Estado" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos los estados</SelectItem>
            {RACE_STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {RACE_STATUS_LABEL[s]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={tipo} onValueChange={(v) => setSearch({ tipo: v })}>
          <SelectTrigger className="w-44" aria-label="Filtrar por tipo">
            <SelectValue placeholder="Tipo" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos los tipos</SelectItem>
            {RACE_TYPES.map((t) => (
              <SelectItem key={t} value={t}>
                {RACE_TYPE_LABEL[t]}
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
          title="Sin carreras"
          description={
            search.q || estado !== "all" || tipo !== "all"
              ? "Ninguna carrera coincide con los filtros aplicados."
              : "Todavía no hay carreras programadas en la liga."
          }
        />
      ) : (
        <div className="overflow-x-auto rounded-xl border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Carrera</TableHead>
                <TableHead>Fecha</TableHead>
                <TableHead>Tipo</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead className="text-right">Distancia</TableHead>
                <TableHead className="text-right">Cupo</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((r) => {
                const d = raceDate(r);
                return (
                  <TableRow
                    key={r.id}
                    tabIndex={0}
                    role="link"
                    className="cursor-pointer"
                    onClick={() => open(r.id)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        open(r.id);
                      }
                    }}
                  >
                    <TableCell className="font-medium">{r.name}</TableCell>
                    <TableCell>
                      {d ? dateFmt.format(d) : "—"}
                      <span className="block text-xs text-muted-foreground">
                        {r.time?.slice(0, 5)}
                      </span>
                    </TableCell>
                    <TableCell>{RACE_TYPE_LABEL[r.type] ?? r.type}</TableCell>
                    <TableCell>
                      <StatusBadge status={r.status} />
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {r.distanceMeters} m
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{r.maxPlayers}</TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}
    </>
  );
}