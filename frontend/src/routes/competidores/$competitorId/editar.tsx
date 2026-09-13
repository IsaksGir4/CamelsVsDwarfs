import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { CompetitorForm, type CompetitorFormValues } from "@/components/CompetitorForm";
import { LoadingState, ErrorState } from "@/components/states";
import {
  competitorQuery,
  updateCompetitor,
  type Competitor,
  type CompetitorInput,
} from "@/lib/league-api";
import { ApiError, errorMessage } from "@/lib/api";

export const Route = createFileRoute("/competidores/$competitorId/editar")({
  component: EditCompetitorPage,
  errorComponent: ({ error }) => <div role="alert">{error instanceof Error ? error.message : "Error inesperado"}</div>,
});

const VALID_TYPES = ["DWARF", "CAMEL", "MEDIUM", "OTHER"] as const;

function toFormValues(c: Competitor): Partial<CompetitorFormValues> {
  const isValidType = (VALID_TYPES as readonly string[]).includes(c.type);
  return {
    name: c.name,
    nickname: c.nickname,
    ...(isValidType ? { playerType: c.type as CompetitorFormValues["playerType"] } : {}),
    birthDate: c.birthDate ? c.birthDate.slice(0, 10) : "",
    height: c.height != null ? String(c.height) : "",
    weight: c.weight != null ? String(c.weight) : "",
    placeOfBirth: c.origin,
  };
}

function EditCompetitorPage() {
  const { competitorId } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const query = useQuery(competitorQuery(competitorId));

  async function handleSubmit(values: CompetitorInput) {
    try {
      await updateCompetitor(competitorId, values);
      await queryClient.invalidateQueries({ queryKey: ["competitors"] });
      toast.success("Competidor actualizado correctamente.");
      await navigate({ to: "/competidores/$competitorId", params: { competitorId } });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) throw error;
      toast.error(errorMessage(error));
    }
  }

  return (
    <RequireAuth permission="competitors:write">
      <AppLayout>
        <PageHeader
          title="Editar competidor"
          description="El estado y las estadísticas se gestionan desde la ficha del competidor."
        />
        {query.isPending ? (
          <LoadingState rows={4} />
        ) : query.isError ? (
          <ErrorState message={errorMessage(query.error)} onRetry={() => void query.refetch()} />
        ) : (
          <CompetitorForm
            defaultValues={toFormValues(query.data)}
            submitLabel="Guardar cambios"
            cancelTo="/competidores"
            onSubmit={handleSubmit}
          />
        )}
      </AppLayout>
    </RequireAuth>
  );
}