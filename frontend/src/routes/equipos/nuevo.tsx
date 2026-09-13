import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { TeamForm } from "@/components/TeamForm";
import { createTeam, type TeamInput } from "@/lib/league-api";
import { ApiError, errorMessage } from "@/lib/api";

export const Route = createFileRoute("/equipos/nuevo")({
  component: NewTeamPage,
  errorComponent: ({ error }) => <div role="alert">{error instanceof Error ? error.message : "Error inesperado"}</div>,
});

function NewTeamPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  async function handleSubmit(values: TeamInput) {
    try {
      const created = await createTeam(values);
      await queryClient.invalidateQueries({ queryKey: ["teams"] });
      toast.success("Equipo creado correctamente.");
      if (created?.idTeam) {
        await navigate({ to: "/equipos/$teamId", params: { teamId: created.idTeam } });
      } else {
        await navigate({ to: "/equipos" });
      }
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) throw error;
      toast.error(errorMessage(error));
    }
  }

  return (
    <RequireAuth permission="teams:write">
      <AppLayout>
        <PageHeader
          title="Nuevo equipo"
          description="Registra una escudería. Se crea en estado activo y sin miembros."
        />
        <TeamForm submitLabel="Crear equipo" cancelTo="/equipos" onSubmit={handleSubmit} />
      </AppLayout>
    </RequireAuth>
  );
}