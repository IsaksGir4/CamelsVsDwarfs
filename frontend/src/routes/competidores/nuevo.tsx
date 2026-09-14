import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { CompetitorForm } from "@/components/CompetitorForm";
import { createCompetitor, type CompetitorInput } from "@/lib/league-api";
import { ApiError, errorMessage } from "@/lib/api";

export const Route = createFileRoute("/competidores/nuevo")({
  head: () => ({
    meta: [
      { title: "Nuevo competidor | EIA Camel vs. Dwarf Racing" },
      { name: "description", content: "Registra un nuevo camello o enano en la liga." },
      { property: "og:title", content: "Nuevo competidor | EIA Camel vs. Dwarf Racing" },
      { property: "og:description", content: "Registra un nuevo competidor en la liga." },
    ],
  }),
  component: NewCompetitorPage,
  errorComponent: ({ error }) => <div role="alert">{error instanceof Error ? error.message : "Error inesperado"}</div>,
});

function NewCompetitorPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  async function handleSubmit(values: CompetitorInput) {
    try {
      const created = await createCompetitor(values);
      await queryClient.invalidateQueries({ queryKey: ["competitors"] });
      toast.success("Competidor creado correctamente.");
      if (created?.idPlayer) {
        await navigate({ to: "/competidores/$competitorId", params: { competitorId: created.idPlayer } });
      } else {
        await navigate({ to: "/competidores" });
      }
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) throw error;
      toast.error(errorMessage(error));
    }
  }

  return (
    <RequireAuth permission="competitors:write">
      <AppLayout>
        <PageHeader
          title="Nuevo competidor"
          description="Registra un camello o un enano en la liga."
        />
        <CompetitorForm
          submitLabel="Crear competidor"
          cancelTo="/competidores"
          onSubmit={handleSubmit}
        />
      </AppLayout>
    </RequireAuth>
  );
}