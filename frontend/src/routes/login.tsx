import { useEffect } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { LogIn, AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Iniciar sesión | EIA Camel vs. Dwarf Racing League" },
      {
        name: "description",
        content:
          "Accede con tu cuenta de la liga para administrar competidores, equipos y carreras de camellos y enanos.",
      },
      { property: "og:title", content: "Iniciar sesión | EIA Racing League" },
      {
        property: "og:description",
        content: "Accede con tu cuenta para administrar la liga de camellos y enanos.",
      },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const { status, error, login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (status === "authenticated") void navigate({ to: "/", replace: true });
  }, [status, navigate]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-secondary px-4">
      <div className="w-full max-w-md rounded-xl border bg-card p-8 shadow-sm">
        <p className="font-display text-xs uppercase tracking-[0.3em] text-muted-foreground">
          The Great EIA
        </p>
        <h1 className="page-title mt-1">
          <span className="text-camel">Camel</span> vs. <span className="text-dwarf">Dwarf</span>{" "}
          Racing League
        </h1>
        <p className="mt-3 text-sm text-muted-foreground">
          Panel de administración de la liga. Inicia sesión para continuar.
        </p>

        {status === "error" && (
          <div
            role="alert"
            className="mt-6 flex gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm"
          >
            <AlertTriangle className="mt-0.5 size-4 shrink-0 text-destructive" aria-hidden />
            <span>{error ?? "No se pudo contactar con el servidor de autenticación."}</span>
          </div>
        )}

        <Button
          className="mt-6 w-full gap-2"
          size="lg"
          onClick={login}
          disabled={status === "loading"}
        >
          <LogIn className="size-4" aria-hidden />
          {status === "loading" ? "Comprobando sesión…" : "Entrar con tu cuenta"}
        </Button>

        <p className="mt-4 text-xs text-muted-foreground">
          Te redirigiremos al proveedor de identidad de la liga para autenticarte de forma segura.
        </p>
      </div>
    </div>
  );
}
