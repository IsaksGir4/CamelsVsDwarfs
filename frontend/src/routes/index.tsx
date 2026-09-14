import { createFileRoute, Link } from "@tanstack/react-router";
import { useQueries } from "@tanstack/react-query";
import { Flag, Trophy, Users, Shield, ArrowRight } from "lucide-react";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { StatusBadge } from "@/components/StatusBadge";
import { EmptyState, ErrorState, LoadingState } from "@/components/states";
import { useAuth } from "@/lib/auth";
import {
  competitorsQuery,
  teamsQuery,
  racesQuery,
  playerStandingsQuery,
  raceDate,
  UPCOMING_STATUSES,
} from "@/lib/league-api";
import { errorMessage } from "@/lib/api";

export const Route = createFileRoute("/")({
  component: DashboardPage,
  errorComponent: ({ error }) => <div role="alert">{error instanceof Error ? error.message : "Error inesperado"}</div>,
});

function DashboardPage() {
  return (
    <RequireAuth>
      <AppLayout>
        <DashboardContent />
      </AppLayout>
    </RequireAuth>
  );
}

const dateFmt = new Intl.DateTimeFormat("es", { day: "numeric", month: "short", year: "numeric" });

function formatDate(d: Date | null): string {
  return d ? dateFmt.format(d) : "Fecha por confirmar";
}

function DashboardContent() {
  const { user } = useAuth();
  const [competitors, teams, races, standings] = useQueries({
    queries: [competitorsQuery(), teamsQuery(), racesQuery(), playerStandingsQuery()],
  });

  const queries = [competitors, teams, races, standings];
  const failed = queries.find((q) => q.isError);
  const loading = queries.some((q) => q.isPending);

  if (loading) {
    return (
      <>
        <PageHeader
          title={`Hola, ${user?.name ?? "corredor"}`}
          description="Resumen de la temporada de la liga de camellos y enanos."
        />
        <LoadingState rows={6} />
      </>
    );
  }

  if (failed) {
    return (
      <>
        <PageHeader title="Panel de la liga" description="Resumen de la temporada." />
        <ErrorState
          message={errorMessage(failed.error)}
          onRetry={() => void Promise.all(queries.map((q) => q.refetch()))}
        />
      </>
    );
  }

  const allRaces = races.data ?? [];
  const now = new Date();

  const upcoming = allRaces
    .filter((r) => {
      const d = raceDate(r);
      const isUpcomingStatus = UPCOMING_STATUSES.includes(r.status);
      const isFutureDraft =
        r.status === "DRAFT" && d !== null && d >= now;
      return isUpcomingStatus || isFutureDraft;
    })
    .sort((a, b) => (raceDate(a)?.getTime() ?? Infinity) - (raceDate(b)?.getTime() ?? Infinity));

  const completed = allRaces
    .filter((r) => r.status === "COMPLETED")
    .sort((a, b) => (raceDate(b)?.getTime() ?? 0) - (raceDate(a)?.getTime() ?? 0));

  const cards = [
    {
      label: "Competidores activos",
      icon: Users,
      value: (competitors.data ?? []).filter((c) => c.status === "ACTIVE").length,
      hint: "Camellos y enanos inscritos",
    },
    {
      label: "Equipos",
      icon: Shield,
      value: (teams.data ?? []).length,
      hint: "Escuderías registradas",
    },
    {
      label: "Carreras próximas",
      icon: Flag,
      value: upcoming.length,
      hint: "Pruebas por disputarse",
    },
    {
      label: "Carreras completadas",
      icon: Trophy,
      value: completed.length,
      hint: "Resultados cerrados",
    },
  ];

  const topStandings = [...(standings.data ?? [])]
    .sort((a, b) => b.points - a.points)
    .slice(0, 5);

  return (
    <>
      <PageHeader
        title={`Hola, ${user?.name ?? "corredor"}`}
        description="Resumen de la temporada de la liga de camellos y enanos."
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map(({ label, icon: Icon, value, hint }) => (
          <div key={label} className="rounded-xl border bg-card p-5">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-muted-foreground">{label}</span>
              <Icon className="size-4 text-camel" aria-hidden />
            </div>
            <p className="mt-3 font-display text-3xl">{value}</p>
            <p className="mt-1 text-xs text-muted-foreground">{hint}</p>
          </div>
        ))}
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <section className="rounded-xl border bg-card p-5" aria-labelledby="proximas">
          <div className="flex items-center justify-between">
            <h2 id="proximas" className="text-base font-semibold">
              Próximas carreras
            </h2>
            <Link
              to="/carreras"
              className="inline-flex items-center gap-1 text-sm text-camel hover:underline"
            >
              Ver todas <ArrowRight className="size-3.5" aria-hidden />
            </Link>
          </div>
          <div className="mt-4">
            {upcoming.length === 0 ? (
              <EmptyState
                title="Todavía no hay carreras"
                description="Cuando se creen carreras en la liga aparecerán aquí con su estado."
              />
            ) : (
              <ul className="divide-y">
                {upcoming.slice(0, 5).map((r) => (
                  <li key={r.id} className="flex items-center justify-between gap-3 py-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{r.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {formatDate(raceDate(r))}
                        {r.start ? ` · ${r.start}` : ""}
                      </p>
                    </div>
                    <StatusBadge status={r.status} />
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        <section className="rounded-xl border bg-card p-5" aria-labelledby="top-standing">
          <div className="flex items-center justify-between">
            <h2 id="top-standing" className="text-base font-semibold">
              Top 5 de la clasificación
            </h2>
            <Link
              to="/standings"
              className="inline-flex items-center gap-1 text-sm text-camel hover:underline"
            >
              Ver todo <ArrowRight className="size-3.5" aria-hidden />
            </Link>
          </div>
          <div className="mt-4">
            {topStandings.length === 0 ? (
              <EmptyState
                title="Sin clasificación todavía"
                description="La clasificación se calculará cuando haya resultados registrados."
              />
            ) : (
              <ol className="divide-y">
                {topStandings.map((s, i) => (
                  <li key={s.playerId} className="flex items-center gap-3 py-3">
                    <span className="w-6 text-center font-display text-lg text-muted-foreground">
                      {i + 1}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{s.name}</p>
                      <p className="text-xs text-muted-foreground">{s.nickname}</p>
                    </div>
                    <span className="font-display text-lg">{s.points} pts</span>
                  </li>
                ))}
              </ol>
            )}
          </div>
        </section>
      </div>

      <section className="mt-6 rounded-xl border bg-card p-5" aria-labelledby="ultimas-finalizadas">
        <h2 id="ultimas-finalizadas" className="text-base font-semibold">
          Últimas carreras finalizadas
        </h2>
        <div className="mt-4">
          {completed.length === 0 ? (
            <EmptyState
              title="Aún no hay carreras finalizadas"
              description="Las carreras aparecerán aquí cuando se registren sus resultados oficiales."
            />
          ) : (
            <ul className="divide-y">
              {completed.slice(0, 5).map((r) => (
                <li key={r.id} className="flex items-center justify-between gap-3 py-3">
                  <div className="min-w-0">
                    <Link
                      to="/carreras/$raceId"
                      params={{ raceId: r.id }}
                      className="truncate text-sm font-medium hover:underline"
                    >
                      {r.name}
                    </Link>
                    <p className="text-xs text-muted-foreground">
                      {formatDate(raceDate(r))} · {r.distanceMeters} m
                    </p>
                  </div>
                  <StatusBadge status={r.status} />
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </>
  );
}