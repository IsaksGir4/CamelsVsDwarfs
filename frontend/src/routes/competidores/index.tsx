import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { z } from "zod";
import { useMemo } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown, Plus, Search } from "lucide-react";
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
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import { useAuth } from "@/lib/auth";
import {
  competitorsQuery,
  competitorName,
  competitorNickname,
  competitorTypeLabel,
  competitorWins,
  competitorLosses,
  competitorRacesCompleted,
  type Competitor,
} from "@/lib/league-api";
import { errorMessage } from "@/lib/api";

const competitorSearchSchema = z.object({
  page: z.number().int().catch(1).default(1),
  q: z.string().catch("").default(""),
  tipo: z.string().catch("all").default("all"),
  estado: z.string().catch("all").default("all"),
  sort: z.string().catch("nombre").default("nombre"),
  dir: z.string().catch("asc").default("asc"),
});

export const Route = createFileRoute("/competidores/")({
  validateSearch: competitorSearchSchema,   // sin zodValidator()
  head: () => ({
    meta: [
      { title: "Competidores | EIA Camel vs. Dwarf Racing" },
      {
        name: "description",
        content: "Consulta y administra los camellos y enanos que compiten en la liga.",
      },
      { property: "og:title", content: "Competidores | EIA Camel vs. Dwarf Racing" },
      {
        property: "og:description",
        content: "Consulta y administra los competidores de la liga.",
      },
    ],
  }),
  component: CompetitorsPage,
  errorComponent: ({ error }) => <div role="alert">{error.message}</div>,
});

const PAGE_SIZE = 10;

const COMPETITOR_TYPES = ["CAMEL", "DWARF"];
const COMPETITOR_STATUSES = ["ACTIVE", "INJURED", "SUSPENDED", "RETIRED"];

const STATUS_FILTER_LABEL: Record<string, string> = {
  ACTIVE: "Activo",
  INJURED: "Lesionado",
  SUSPENDED: "Suspendido",
  RETIRED: "Retirado",
};

type SortKey = "nombre" | "apodo" | "tipo" | "estado" | "victorias" | "derrotas" | "completadas";

const SORT_KEYS: SortKey[] = [
  "nombre",
  "apodo",
  "tipo",
  "estado",
  "victorias",
  "derrotas",
  "completadas",
];

function sortValue(c: Competitor, key: SortKey): string | number {
  switch (key) {
    case "nombre":
      return competitorName(c).toLowerCase();
    case "apodo":
      return competitorNickname(c).toLowerCase();
    case "tipo":
      return competitorTypeLabel(c.type);
    case "estado":
      return c.status ?? "";
    case "victorias":
      return competitorWins(c);
    case "derrotas":
      return competitorLosses(c);
    case "completadas":
      return competitorRacesCompleted(c);
  }
}

function CompetitorsPage() {
  return (
    <RequireAuth>
      <AppLayout>
        <CompetitorsContent />
      </AppLayout>
    </RequireAuth>
  );
}

function CompetitorsContent() {
  const { data, isPending, isError, error, refetch } = useQuery(competitorsQuery());
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/competidores/" });
  const { hasPermission } = useAuth();

  const sortKey: SortKey = SORT_KEYS.includes(search.sort as SortKey)
    ? (search.sort as SortKey)
    : "nombre";
  const dir = search.dir === "desc" ? "desc" : "asc";
  const tipo = ["all", ...COMPETITOR_TYPES].includes(search.tipo) ? search.tipo : "all";
  const estado = ["all", ...COMPETITOR_STATUSES].includes(search.estado) ? search.estado : "all";

  const setSearch = (patch: Partial<typeof search>) =>
    navigate({ search: (prev) => ({ ...prev, ...patch }) });

  const filtered = useMemo(() => {
    if (!data) return [];
    const q = search.q.trim().toLowerCase();
    let list = data.filter((c) => {
      if (tipo !== "all" && c.type !== tipo) return false;
      if (estado !== "all" && c.status !== estado) return false;
      if (q && !competitorName(c).toLowerCase().includes(q)) return false;
      return true;
    });
    list = [...list].sort((a, b) => {
      const va = sortValue(a, sortKey);
      const vb = sortValue(b, sortKey);
      const cmp =
        typeof va === "number" && typeof vb === "number"
          ? va - vb
          : String(va).localeCompare(String(vb), "es");
      return dir === "asc" ? cmp : -cmp;
    });
    return list;
  }, [data, search.q, tipo, estado, sortKey, dir]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const page = Math.min(Math.max(1, search.page), totalPages);
  const pageRows = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const toggleSort = (key: SortKey) => {
    if (key === sortKey) {
      setSearch({ dir: dir === "asc" ? "desc" : "asc", page: 1 });
    } else {
      setSearch({ sort: key, dir: "asc", page: 1 });
    }
  };

  const sortHeader = (key: SortKey, label: string, className?: string) => (
    <TableHead className={className}>
      <button
        type="button"
        onClick={() => toggleSort(key)}
        className="inline-flex items-center gap-1 font-medium hover:text-foreground"
        aria-label={`Ordenar por ${label}`}
      >
        {label}
        {sortKey !== key ? (
          <ArrowUpDown className="size-3.5 opacity-50" aria-hidden />
        ) : dir === "asc" ? (
          <ArrowUp className="size-3.5" aria-hidden />
        ) : (
          <ArrowDown className="size-3.5" aria-hidden />
        )}
      </button>
    </TableHead>
  );

  return (
    <>
      <PageHeader
        title="Competidores"
        description="Camellos y enanos registrados en la liga."
        actions={
          hasPermission("competitors:write") ? (
            <Button onClick={() => navigate({ to: "/competidores/nuevo" })}>
              <Plus className="size-4" aria-hidden />
              Nuevo competidor
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
            onChange={(e) => setSearch({ q: e.target.value, page: 1 })}
            placeholder="Buscar por nombre…"
            aria-label="Buscar por nombre"
            className="pl-9"
          />
        </div>
        <Select value={tipo} onValueChange={(v) => setSearch({ tipo: v, page: 1 })}>
          <SelectTrigger className="w-40" aria-label="Filtrar por tipo">
            <SelectValue placeholder="Tipo" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos los tipos</SelectItem>
            <SelectItem value="CAMEL">Camello</SelectItem>
            <SelectItem value="DWARF">Enano</SelectItem>
          </SelectContent>
        </Select>
        <Select value={estado} onValueChange={(v) => setSearch({ estado: v, page: 1 })}>
          <SelectTrigger className="w-44" aria-label="Filtrar por estado">
            <SelectValue placeholder="Estado" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos los estados</SelectItem>
            {COMPETITOR_STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {STATUS_FILTER_LABEL[s]}
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
          title="Sin competidores"
          description={
            search.q || tipo !== "all" || estado !== "all"
              ? "Ningún competidor coincide con los filtros aplicados. Prueba a ajustarlos."
              : "Todavía no hay competidores registrados en la liga."
          }
        />
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  {sortHeader("nombre", "Nombre")}
                  {sortHeader("apodo", "Apodo")}
                  {sortHeader("tipo", "Tipo")}
                  {sortHeader("estado", "Estado")}
                  {sortHeader("victorias", "Victorias", "text-right")}
                  {sortHeader("derrotas", "Derrotas", "text-right")}
                  {sortHeader("completadas", "Carreras", "text-right")}
                </TableRow>
              </TableHeader>
              <TableBody>
                {pageRows.map((c) => (
                  <TableRow
                    key={c.id}
                    tabIndex={0}
                    role="link"
                    className="cursor-pointer"
                    onClick={() =>
                      navigate({ to: "/competidores/$competitorId", params: { competitorId: c.id } })
                    }
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        navigate({
                          to: "/competidores/$competitorId",
                          params: { competitorId: c.id },
                        });
                      }
                    }}
                  >
                    <TableCell className="font-medium">{competitorName(c)}</TableCell>
                    <TableCell>{competitorNickname(c)}</TableCell>
                    <TableCell>{competitorTypeLabel(c.type)}</TableCell>
                    <TableCell>
                      {c.status ? <StatusBadge status={c.status} /> : "—"}
                    </TableCell>
                    <TableCell className="text-right">{competitorWins(c)}</TableCell>
                    <TableCell className="text-right">{competitorLosses(c)}</TableCell>
                    <TableCell className="text-right">{competitorRacesCompleted(c)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {totalPages > 1 && (
            <Pagination className="mt-4">
              <PaginationContent>
                <PaginationItem>
                  <PaginationPrevious
                    href="#"
                    aria-disabled={page <= 1}
                    className={page <= 1 ? "pointer-events-none opacity-50" : ""}
                    onClick={(e) => {
                      e.preventDefault();
                      if (page > 1) setSearch({ page: page - 1 });
                    }}
                  />
                </PaginationItem>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                  <PaginationItem key={p}>
                    <PaginationLink
                      href="#"
                      isActive={p === page}
                      onClick={(e) => {
                        e.preventDefault();
                        setSearch({ page: p });
                      }}
                    >
                      {p}
                    </PaginationLink>
                  </PaginationItem>
                ))}
                <PaginationItem>
                  <PaginationNext
                    href="#"
                    aria-disabled={page >= totalPages}
                    className={page >= totalPages ? "pointer-events-none opacity-50" : ""}
                    onClick={(e) => {
                      e.preventDefault();
                      if (page < totalPages) setSearch({ page: page + 1 });
                    }}
                  />
                </PaginationItem>
              </PaginationContent>
            </Pagination>
          )}
          <p className="mt-2 text-xs text-muted-foreground">
            {filtered.length} competidor{filtered.length === 1 ? "" : "es"} · página {page} de{" "}
            {totalPages}
          </p>
        </>
      )}
    </>
  );
}
