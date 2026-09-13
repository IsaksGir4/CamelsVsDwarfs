import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { TeamForm, type TeamFormValues } from "@/components/TeamForm";
import { LoadingState, ErrorState } from "@/components/states";
import {
  teamQuery,
  teamMembersQuery,
  updateTeam,
  type Team,
  type TeamInput,
} from "@/lib/league-api";
import { ApiError, errorMessage } from "@/lib/api";

export const Route = createFileRoute("/equipos/$teamId/editar")({
  component: EditTeamPage,
  errorComponent: ({ error }) => <div role="alert">{error.message}</div>,
});

const VALID_CATEGORIES = ["DUO", "TRIO", "QUARTET"] as const;

function toFormValues(t: Team): Partial<TeamFormValues> {
  const isValid = (VALID_CATEGORIES as readonly string[]).includes(t.category);
  return {
    teamName: t.name,
    description: t.description,
    responsableCoach: t.coach ?? "",
    ...(isValid ? { category: t.category as TeamFormValues["category"] } : {}),
  };
}

function EditTeamPage() {
  const { teamId } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const query = useQuery(teamQuery(teamId));
  const members = useQuery(teamMembersQuery(teamId));

  const activeCount = (members.data ?? []).filter((m) => m.status === "ACTIVE").length;

  async function handleSubmit(values: TeamInput) {
    try {
      await updateTeam(teamId, values);
      await queryClient.invalidateQueries({ queryKey: ["teams"] });
      toast.success("Equipo actualizado correctamente.");
      await navigate({ to: "/equipos/$teamId", params: { teamId } });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) throw error;
      toast.error(errorMessage(error));
    }
  }

  return (
    <RequireAuth permission="teams:write">
      <AppLayout>
        <PageHeader
          title="Editar equipo"
          description="El estado y los miembros se gestionan desde la ficha del equipo."
        />
        {query.isPending ? (
          <LoadingState rows={4} />
        ) : query.isError ? (
          <ErrorState message={errorMessage(query.error)} onRetry={() => void query.refetch()} />
        ) : (
          <TeamForm
            defaultValues={toFormValues(query.data)}
            submitLabel="Guardar cambios"
            cancelTo="/equipos"
            onSubmit={handleSubmit}
            categoryHint={
              activeCount > 0
                ? `El equipo tiene ${activeCount} miembro${activeCount === 1 ? "" : "s"} activo${activeCount === 1 ? "" : "s"}: no se puede reducir la categoría por debajo de ese número.`
                : "La categoría determina el máximo de miembros del equipo."
            }
          />
        )}
      </AppLayout>
    </RequireAuth>
  );
}