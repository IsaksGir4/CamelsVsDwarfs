import { createFileRoute } from "@tanstack/react-router";
import { AccessDenied } from "@/components/AccessDenied";

export const Route = createFileRoute("/denegado")({
  head: () => ({
    meta: [
      { title: "Acceso denegado | EIA Racing League" },
      {
        name: "description",
        content: "No tienes permisos para acceder a esta sección de la liga.",
      },
      { property: "og:title", content: "Acceso denegado | EIA Racing League" },
      {
        property: "og:description",
        content: "No tienes permisos para acceder a esta sección de la liga.",
      },
    ],
  }),
  component: AccessDenied,
});