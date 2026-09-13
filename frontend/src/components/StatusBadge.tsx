import { cn } from "@/lib/utils";

type Tone = "green" | "amber" | "orange" | "gray" | "red" | "blue" | "indigo";

const TONE_CLASS: Record<Tone, string> = {
  green: "bg-status-green text-status-green-foreground",
  amber: "bg-status-amber text-status-amber-foreground",
  orange: "bg-status-orange text-status-orange-foreground",
  gray: "bg-status-gray text-status-gray-foreground",
  red: "bg-status-red text-status-red-foreground",
  blue: "bg-status-blue text-status-blue-foreground",
  indigo: "bg-status-indigo text-status-indigo-foreground",
};

const STATUS_TONE: Record<string, Tone> = {
  // Competidor
  ACTIVE: "green",
  INJURED: "amber",
  SUSPENDED: "orange",
  RETIRED: "gray",
  // Equipo
  INACTIVE: "gray",
  DISBANDED: "red",
  // Carrera
  DRAFT: "gray",
  OPEN_FOR_REGISTRATION: "blue",
  CLOSED_FOR_REGISTRATION: "indigo",
  IN_PROGRESS: "amber",
  COMPLETED: "green",
  CANCELLED: "red",
  // Inscripción
  PENDING: "amber",
  APPROVED: "green",
  REJECTED: "red",
  // Resultado
  FINISHED: "green",
  DISQUALIFIED: "red",
  DID_NOT_FINISH: "orange",
  DID_NOT_START: "gray",
};

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Activo",
  INJURED: "Lesionado",
  SUSPENDED: "Suspendido",
  RETIRED: "Retirado",
  INACTIVE: "Inactivo",
  DISBANDED: "Disuelto",
  DRAFT: "Borrador",
  OPEN_FOR_REGISTRATION: "Inscripción abierta",
  CLOSED_FOR_REGISTRATION: "Inscripción cerrada",
  IN_PROGRESS: "En curso",
  COMPLETED: "Completada",
  CANCELLED: "Cancelada",
  PENDING: "Pendiente",
  APPROVED: "Aprobada",
  REJECTED: "Rechazada",
  FINISHED: "Finalizó",
  DISQUALIFIED: "Descalificado",
  DID_NOT_FINISH: "No terminó",
  DID_NOT_START: "No salió",
};

export function StatusBadge({ status, className }: { status: string; className?: string }) {
  const tone = STATUS_TONE[status] ?? "gray";
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium",
        TONE_CLASS[tone],
        className,
      )}
    >
      {status === "IN_PROGRESS" && (
        <span className="size-1.5 animate-pulse rounded-full bg-current" aria-hidden />
      )}
      {STATUS_LABEL[status] ?? status}
    </span>
  );
}