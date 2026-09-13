import { Link } from "@tanstack/react-router";
import { ShieldX } from "lucide-react";
import { Button } from "@/components/ui/button";

export function AccessDenied() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center px-4 text-center">
      <ShieldX className="mb-4 size-10 text-destructive" aria-hidden />
      <h1 className="page-title">Acceso denegado</h1>
      <p className="mt-2 max-w-sm text-sm text-muted-foreground">
        Tu rol no tiene permiso para ver esta sección de la liga. Si crees que es un error, habla
        con un administrador.
      </p>
      <Button asChild className="mt-6">
        <Link to="/">Volver al panel</Link>
      </Button>
    </div>
  );
}
