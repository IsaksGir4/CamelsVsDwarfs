import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { AppLayout } from "@/components/AppLayout";
import { RequireAuth } from "@/components/RequireAuth";
import { PageHeader } from "@/components/PageHeader";
import { EmptyState, ErrorState, LoadingState } from "@/components/states";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { errorMessage } from "@/lib/api";
import { auditQuery } from "@/lib/league-api";

export const Route = createFileRoute("/audit-log")({
  component: AuditLogPage,
  errorComponent: ({ error }) => <div role="alert">{error instanceof Error ? error.message : "Error inesperado"}</div>,
});

const dateTimeFmt = new Intl.DateTimeFormat("es", {
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

function formatDateTime(raw?: string): string {
  if (!raw) return "—";
  const d = new Date(raw);
  return Number.isNaN(d.getTime()) ? "—" : dateTimeFmt.format(d);
}

function AuditLogPage() {
  return (
    <RequireAuth permission="audit:read">
      <AppLayout>
        <AuditLogContent />
      </AppLayout>
    </RequireAuth>
  );
}

function AuditLogContent() {
  const [page, setPage] = useState(0);
  const { data, isPending, isError, error, refetch } = useQuery(auditQuery(page));

  const rows = data ?? [];

  return (
    <>
      <PageHeader
        title="Registro de auditoría"
        description="Acciones relevantes ejecutadas en la liga. Solo visible para administradores."
      />

      {isPending ? (
        <LoadingState rows={6} />
      ) : isError ? (
        <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
      ) : rows.length === 0 ? (
        <EmptyState
          title="Sin registros"
          description={
            page === 0
              ? "Todavía no se ha registrado ninguna acción auditable."
              : "No hay más registros en esta página."
          }
        />
      ) : (
        <div className="overflow-x-auto rounded-xl border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Fecha</TableHead>
                <TableHead>Usuario</TableHead>
                <TableHead>Acción</TableHead>
                <TableHead>Entidad</TableHead>
                <TableHead>Descripción</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((entry, i) => (
                <TableRow key={entry.idAuditLog ?? i}>
                  <TableCell className="whitespace-nowrap text-sm">
                    {formatDateTime(entry.timestamp)}
                  </TableCell>
                  <TableCell className="font-medium">{entry.username ?? "—"}</TableCell>
                  <TableCell>
                    <span className="rounded bg-muted px-2 py-0.5 font-mono text-xs">
                      {entry.action ?? "—"}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {entry.entityType ?? "—"}
                  </TableCell>
                  <TableCell className="max-w-72 truncate text-sm">
                    {entry.description ?? "—"}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <div className="mt-4 flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={page === 0 || isPending}
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          Anterior
        </Button>
        <span className="text-sm text-muted-foreground">Página {page + 1}</span>
        <Button
          variant="outline"
          size="sm"
          disabled={isPending || rows.length < 20}
          onClick={() => setPage((p) => p + 1)}
        >
          Siguiente
        </Button>
      </div>
    </>
  );
}