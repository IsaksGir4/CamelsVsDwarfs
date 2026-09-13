import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { RaceForm } from "@/components/RaceForm";
import { createRace, type RaceInput } from "@/lib/league-api";
import { ApiError, errorMessage } from "@/lib/api";

export const Route = createFileRoute("/carreras/nueva")({
  component: NewRacePage,
  errorComponent: ({ error }) => <div role="alert">{error instanceof Error ? error.message : "Error inesperado"}</div>,
});

function NewRacePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  async function handleSubmit(values: RaceInput) {
    try {
      const created = await createRace(values);
      await queryClient.invalidateQueries({ queryKey: ["races"] });
      toast.success("Carrera creada en estado borrador.");
      if (created?.idRace) {
        await navigate({ to: "/carreras/$raceId", params: { raceId: created.idRace } });
      } else {
        await navigate({ to: "/carreras" });
      }
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) throw error;
      toast.error(errorMessage(error));
    }
  }

  return (
    <RequireAuth permission="races:write">
      <AppLayout>
        <PageHeader
          title="Nueva carrera"
          description="Programa una prueba de la temporada. Se creará en estado borrador."
        />
        <RaceForm submitLabel="Crear carrera" cancelTo="/carreras" onSubmit={handleSubmit} />
      </AppLayout>
    </RequireAuth>
  );
}