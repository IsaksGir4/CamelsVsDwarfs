import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { RaceForm, type RaceFormValues } from "@/components/RaceForm";
import { LoadingState, ErrorState } from "@/components/states";
import { raceQuery, updateRace, type Race, type RaceInput } from "@/lib/league-api";
import { ApiError, errorMessage } from "@/lib/api";

export const Route = createFileRoute("/carreras/$raceId/editar")({
  component: EditRacePage,
  errorComponent: ({ error }) => <div role="alert">{error.message}</div>,
});

const VALID_TYPES = ["INDIVIDUAL", "TEAM", "MIXED"] as const;

function toFormValues(r: Race): Partial<RaceFormValues> {
  const isValidType = (VALID_TYPES as readonly string[]).includes(r.type);
  return {
    raceName: r.name,
    description: r.description ?? "",
    programationDate: r.date,
    programationHour: r.time ? r.time.slice(0, 5) : "10:00",
    ubicationStart: r.start,
    ubicationFinish: r.finish,
    distanceMeters: String(r.distanceMeters),
    maxPlayers: String(r.maxPlayers),
    ...(isValidType ? { raceType: r.type as RaceFormValues["raceType"] } : {}),
    registrationDeadline: r.registrationDeadline,
  };
}

function EditRacePage() {
  const { raceId } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const query = useQuery(raceQuery(raceId));

  async function handleSubmit(values: RaceInput) {
    try {
      await updateRace(raceId, values);
      await queryClient.invalidateQueries({ queryKey: ["races"] });
      toast.success("Carrera actualizada correctamente.");
      await navigate({ to: "/carreras/$raceId", params: { raceId } });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) throw error;
      toast.error(errorMessage(error));
    }
  }

  return (
    <RequireAuth permission="races:write">
      <AppLayout>
        <PageHeader
          title="Editar carrera"
          description="Solo se pueden editar carreras en borrador o con inscripciones abiertas."
        />
        {query.isPending ? (
          <LoadingState rows={4} />
        ) : query.isError ? (
          <ErrorState message={errorMessage(query.error)} onRetry={() => void query.refetch()} />
        ) : (
          <RaceForm
            defaultValues={toFormValues(query.data)}
            submitLabel="Guardar cambios"
            cancelTo="/carreras"
            onSubmit={handleSubmit}
          />
        )}
      </AppLayout>
    </RequireAuth>
  );
}