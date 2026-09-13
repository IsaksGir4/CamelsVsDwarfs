import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Medal, Trophy } from "lucide-react";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { EmptyState, ErrorState, LoadingState } from "@/components/states";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import { errorMessage } from "@/lib/api";
import { playerStandingsQuery, teamStandingsQuery, POINTS_TABLE } from "@/lib/league-api";

export const Route = createFileRoute("/standings")({
  component: StandingsPage,
  errorComponent: ({ error }) => <div role="alert">{error.message}</div>,
});

type Tab = "competidores" | "equipos";

const PODIUM_CLASS = [
  "text-amber-500",
  "text-slate-400",
  "text-amber-700",
];

function Position({ index }: { index: number }) {
  if (index < 3) {
    return (
      <span className="inline-flex items-center gap-1.5 font-display text-lg">
        <Medal className={cn("size-4", PODIUM_CLASS[index])} aria-hidden />
        {index + 1}
      </span>
    );
  }
  return <span className="font-display text-lg text-muted-foreground">{index + 1}</span>;
}

function StandingsPage() {
  return (
    <RequireAuth>
      <AppLayout>
        <StandingsContent />
      </AppLayout>
    </RequireAuth>
  );
}

function StandingsContent() {
  const [tab, setTab] = useState<Tab>("competidores");
  const players = useQuery(playerStandingsQuery());
  const teams = useQuery(teamStandingsQuery());

  const active = tab === "competidores" ? players : teams;

  return (
    <>
      <PageHeader
        title="Clasificación"
        description="Puntuación acumulada de la temporada según los resultados oficiales."
      />

      <div
        role="tablist"
        aria-label="Tipo de clasificación"
        className="mb-4 inline-flex rounded-lg border bg-card p-1"
      >
        {(["competidores", "equipos"] as Tab[]).map((t) => (
          <Button
            key={t}
            role="tab"
            aria-selected={tab === t}
            variant={tab === t ? "default" : "ghost"}
            size="sm"
            onClick={() => setTab(t)}
            className="capitalize"
          >
            {t}
          </Button>
        ))}
      </div>

      {active.isPending ? (
        <LoadingState rows={6} />
      ) : active.isError ? (
        <ErrorState message={errorMessage(active.error)} onRetry={() => void active.refetch()} />
      ) : tab === "competidores" ? (
        <CompetitorTable rows={players.data ?? []} />
      ) : (
        <TeamTable rows={teams.data ?? []} />
      )}

      <Card className="mt-6 max-w-md">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Trophy className="size-4 text-camel" aria-hidden />
            Sistema de puntos
          </CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="space-y-1.5 text-sm">
            {POINTS_TABLE.map((p) => (
              <div key={p.position} className="flex justify-between gap-4">
                <dt className="text-muted-foreground">{p.position}</dt>
                <dd className="font-medium tabular-nums">{p.points} pts</dd>
              </div>
            ))}
          </dl>
        </CardContent>
      </Card>
    </>
  );
}

function CompetitorTable({
  rows,
}: {
  rows: { playerId: string; name: string; nickname: string; points: number; victories: number; defeats: number; racesCompleted: number }[];
}) {
  const sorted = [...rows].sort((a, b) => b.points - a.points);

  if (sorted.length === 0) {
    return (
      <EmptyState
        title="Sin clasificación todavía"
        description="La clasificación aparecerá cuando se registren resultados oficiales en alguna carrera."
      />
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-16">Pos.</TableHead>
            <TableHead>Competidor</TableHead>
            <TableHead>Apodo</TableHead>
            <TableHead className="text-right">Puntos</TableHead>
            <TableHead className="text-right">Victorias</TableHead>
            <TableHead className="text-right">Derrotas</TableHead>
            <TableHead className="text-right">Carreras</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sorted.map((r, i) => (
            <TableRow key={r.playerId}>
              <TableCell>
                <Position index={i} />
              </TableCell>
              <TableCell className="font-medium">{r.name}</TableCell>
              <TableCell className="text-muted-foreground">{r.nickname}</TableCell>
              <TableCell className="text-right font-display text-lg tabular-nums">
                {r.points}
              </TableCell>
              <TableCell className="text-right tabular-nums">{r.victories}</TableCell>
              <TableCell className="text-right tabular-nums">{r.defeats}</TableCell>
              <TableCell className="text-right tabular-nums">{r.racesCompleted}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function TeamTable({
  rows,
}: {
  rows: { teamId: string; teamName: string; points: number; victories: number; defeats: number }[];
}) {
  const sorted = [...rows].sort((a, b) => b.points - a.points);

  if (sorted.length === 0) {
    return (
      <EmptyState
        title="Sin clasificación de equipos"
        description="Aparecerá cuando algún equipo obtenga resultados oficiales en una carrera."
      />
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-16">Pos.</TableHead>
            <TableHead>Equipo</TableHead>
            <TableHead className="text-right">Puntos</TableHead>
            <TableHead className="text-right">Victorias</TableHead>
            <TableHead className="text-right">Derrotas</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sorted.map((r, i) => (
            <TableRow key={r.teamId}>
              <TableCell>
                <Position index={i} />
              </TableCell>
              <TableCell className="font-medium">{r.teamName}</TableCell>
              <TableCell className="text-right font-display text-lg tabular-nums">
                {r.points}
              </TableCell>
              <TableCell className="text-right tabular-nums">{r.victories}</TableCell>
              <TableCell className="text-right tabular-nums">{r.defeats}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}