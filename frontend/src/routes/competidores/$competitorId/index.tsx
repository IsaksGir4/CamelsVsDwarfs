import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { StatusBadge } from "@/components/StatusBadge";
import { LoadingState, ErrorState } from "@/components/states";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/lib/auth";
import { errorMessage } from "@/lib/api";
import {
  COMPETITOR_STATUSES,
  competitorLosses,
  competitorName,
  competitorNickname,
  competitorQuery,
  competitorRacesCompleted,
  competitorTypeLabel,
  competitorWins,
  updateCompetitorStatus,
  type Competitor,
} from "@/lib/league-api";

export const Route = createFileRoute("/competidores/$competitorId/")({
  component: CompetitorDetailPage,
  errorComponent: ({ error }) => <div role="alert">{error.message}</div>,
  notFoundComponent: () => <div>Competidor no encontrado.</div>,
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

function StatusChanger({ competitor }: { competitor: Competitor }) {
  const queryClient = useQueryClient();
  const [saving, setSaving] = useState(false);

  async function change(next: string) {
    if (next === competitor.status) return;
    setSaving(true);
    try {
      await updateCompetitorStatus(competitor.id, next);
      await queryClient.invalidateQueries({ queryKey: ["competitors"] });
      toast.success("Estado actualizado.");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-2">
      <Label htmlFor="status">Cambiar estado</Label>
      <Select value={competitor.status} onValueChange={(v) => void change(v)} disabled={saving}>
        <SelectTrigger id="status" className="w-56">
          <SelectValue placeholder="Selecciona un estado" />
        </SelectTrigger>
        <SelectContent>
          {COMPETITOR_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              <StatusBadge status={s} />
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <p className="text-xs text-muted-foreground">
        Un competidor con resultados oficiales no se puede eliminar: debe retirarse.
      </p>
    </div>
  );
}

function CompetitorDetailPage() {
  const { competitorId } = Route.useParams();
  const { hasPermission } = useAuth();
  const query = useQuery(competitorQuery(competitorId));
  const canWrite = hasPermission("competitors:write");

  return (
    <RequireAuth>
      <AppLayout>
        <PageHeader
          title={query.data ? competitorName(query.data) : "Detalle de competidor"}
          description="Ficha del competidor."
          actions={
            <>
              <Button variant="outline" asChild>
                <Link to="/competidores">Volver</Link>
              </Button>
              {canWrite && (
                <Button asChild>
                  <Link to="/competidores/$competitorId/editar" params={{ competitorId }}>
                    Editar
                  </Link>
                </Button>
              )}
            </>
          }
        />

        {query.isPending ? (
          <LoadingState rows={4} />
        ) : query.isError ? (
          <ErrorState message={errorMessage(query.error)} onRetry={() => void query.refetch()} />
        ) : (
          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>Datos</CardTitle>
              </CardHeader>
              <CardContent>
                <dl className="grid gap-5 sm:grid-cols-2">
                  <Field label="Nombre" value={competitorName(query.data)} />
                  <Field label="Apodo" value={competitorNickname(query.data)} />
                  <Field label="Tipo" value={competitorTypeLabel(query.data.type)} />
                  <Field
                    label="Fecha de nacimiento"
                    value={formatDate(query.data.birthDate)}
                  />
                  <Field
                    label="Altura"
                    value={query.data.height != null ? `${query.data.height} m` : "—"}
                  />
                  <Field
                    label="Peso"
                    value={query.data.weight != null ? `${query.data.weight} kg` : "—"}
                  />
                  <Field label="Lugar de origen" value={query.data.origin || "—"} />
                  <Field label="Fecha de registro" value={formatDate(query.data.registerDate)} />
                  <div>
                    <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                      Estado
                    </dt>
                    <dd className="mt-1">
                      <StatusBadge status={query.data.status} />
                    </dd>
                  </div>
                </dl>
              </CardContent>
            </Card>

            <div className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Estadísticas</CardTitle>
                </CardHeader>
                <CardContent>
                  <dl className="grid grid-cols-3 gap-3 text-center">
                    <Field label="Victorias" value={String(competitorWins(query.data))} />
                    <Field label="Derrotas" value={String(competitorLosses(query.data))} />
                    <Field
                      label="Carreras"
                      value={String(competitorRacesCompleted(query.data))}
                    />
                  </dl>
                </CardContent>
              </Card>

              {canWrite && (
                <Card>
                  <CardHeader>
                    <CardTitle>Estado</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <StatusChanger competitor={query.data} />
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        )}
      </AppLayout>
    </RequireAuth>
  );
}