import { useForm, type Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link } from "@tanstack/react-router";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ApiError } from "@/lib/api";
import { RACE_TYPE_LABEL, type RaceInput } from "@/lib/league-api";
import { cn } from "@/lib/utils";

const RACE_TYPES = ["INDIVIDUAL", "TEAM", "MIXED"] as const;
type RaceTypeValue = (typeof RACE_TYPES)[number];

export const raceSchema = z
  .object({
    raceName: z
      .string()
      .trim()
      .min(3, "El nombre debe tener al menos 3 caracteres.")
      .max(100, "El nombre no puede superar 100 caracteres."),
    description: z.string().trim().max(512, "Maximo 512 caracteres.").default(""),
    programationDate: z.string().min(1, "La fecha es obligatoria."),
    programationHour: z.string().min(1, "La hora es obligatoria."),
    ubicationStart: z
      .string()
      .trim()
      .min(1, "La ubicacion de inicio es obligatoria.")
      .max(100, "Maximo 100 caracteres."),
    ubicationFinish: z
      .string()
      .trim()
      .min(1, "La ubicacion de meta es obligatoria.")
      .max(100, "Maximo 100 caracteres."),
    distanceMeters: z
      .string()
      .min(1, "La distancia es obligatoria.")
      .refine((v) => Number(v) > 0, "La distancia debe ser mayor que cero."),
    maxPlayers: z
      .string()
      .min(1, "El maximo de participantes es obligatorio.")
      .refine((v) => Number(v) >= 2, "Se requieren al menos 2 participantes."),
    raceType: z.enum(RACE_TYPES, { message: "Selecciona el tipo de carrera." }),
    registrationDeadline: z.string().min(1, "La fecha limite es obligatoria."),
  })
  .refine(
    (v) => new Date(`${v.programationDate}T${v.programationHour}`) > new Date(),
    {
      message: "La carrera no puede programarse en el pasado.",
      path: ["programationDate"],
    },
  )
  .refine(
    (v) => new Date(v.registrationDeadline) < new Date(v.programationDate),
    {
      message: "El cierre de inscripciones debe ser anterior a la fecha de la carrera.",
      path: ["registrationDeadline"],
    },
  );

export interface RaceFormValues {
  raceName: string;
  description: string;
  programationDate: string;
  programationHour: string;
  ubicationStart: string;
  ubicationFinish: string;
  distanceMeters: string;
  maxPlayers: string;
  raceType: RaceTypeValue;
  registrationDeadline: string;
}

function FieldError({ id, message }: { id: string; message?: string | undefined }) {
  if (!message) return null;
  return (
    <p id={id} role="alert" className="text-sm font-medium text-destructive">
      {message}
    </p>
  );
}

export function RaceForm({
  defaultValues,
  submitLabel,
  cancelTo,
  onSubmit,
}: {
  defaultValues?: Partial<RaceFormValues>;
  submitLabel: string;
  cancelTo: string;
  onSubmit: (values: RaceInput) => Promise<unknown>;
}) {
  const form = useForm<RaceFormValues>({
    resolver: zodResolver(raceSchema) as unknown as Resolver<RaceFormValues>,
    mode: "onBlur",
    defaultValues: {
      raceName: "",
      description: "",
      programationDate: "",
      programationHour: "10:00",
      ubicationStart: "",
      ubicationFinish: "",
      distanceMeters: "1000",
      maxPlayers: "6",
      registrationDeadline: "",
      ...defaultValues,
    } as RaceFormValues,
  });

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = form;

  const raceType = watch("raceType");
  const today = new Date().toISOString().slice(0, 10);

  const submit = handleSubmit(async (values) => {
    try {
      await onSubmit({
        raceName: values.raceName,
        description: values.description,
        programationDate: values.programationDate,
        programationHour:
          values.programationHour.length === 5
            ? `${values.programationHour}:00`
            : values.programationHour,
        ubicationStart: values.ubicationStart,
        ubicationFinish: values.ubicationFinish,
        distanceMeters: Number(values.distanceMeters),
        maxPlayers: Number(values.maxPlayers),
        raceType: values.raceType,
        registrationDeadline: values.registrationDeadline,
      });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setError("raceName", { type: "server", message: error.message }, { shouldFocus: true });
        return;
      }
      throw error;
    }
  });

  return (
    <form onSubmit={submit} noValidate className="max-w-2xl space-y-6">
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="raceName">Nombre de la carrera</Label>
          <Input
            id="raceName"
            autoComplete="off"
            aria-invalid={!!errors.raceName}
            aria-describedby={errors.raceName ? "raceName-error" : undefined}
            {...register("raceName")}
          />
          <FieldError id="raceName-error" message={errors.raceName?.message} />
        </div>

        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="description">Descripción</Label>
          <Input
            id="description"
            autoComplete="off"
            placeholder="Opcional"
            aria-invalid={!!errors.description}
            {...register("description")}
          />
          <FieldError id="description-error" message={errors.description?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="programationDate">Fecha</Label>
          <Input
            id="programationDate"
            type="date"
            min={today}
            aria-invalid={!!errors.programationDate}
            aria-describedby={errors.programationDate ? "programationDate-error" : undefined}
            {...register("programationDate")}
          />
          <FieldError id="programationDate-error" message={errors.programationDate?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="programationHour">Hora de salida</Label>
          <Input
            id="programationHour"
            type="time"
            aria-invalid={!!errors.programationHour}
            {...register("programationHour")}
          />
          <FieldError id="programationHour-error" message={errors.programationHour?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="ubicationStart">Punto de salida</Label>
          <Input id="ubicationStart" autoComplete="off" {...register("ubicationStart")} />
          <FieldError id="ubicationStart-error" message={errors.ubicationStart?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="ubicationFinish">Meta</Label>
          <Input id="ubicationFinish" autoComplete="off" {...register("ubicationFinish")} />
          <FieldError id="ubicationFinish-error" message={errors.ubicationFinish?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="distanceMeters">Distancia (m)</Label>
          <Input
            id="distanceMeters"
            type="number"
            min="1"
            aria-invalid={!!errors.distanceMeters}
            {...register("distanceMeters")}
          />
          <FieldError id="distanceMeters-error" message={errors.distanceMeters?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="maxPlayers">Máximo de participantes</Label>
          <Input
            id="maxPlayers"
            type="number"
            min="2"
            aria-invalid={!!errors.maxPlayers}
            {...register("maxPlayers")}
          />
          <FieldError id="maxPlayers-error" message={errors.maxPlayers?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="raceType">Tipo de carrera</Label>
          <Select
            value={raceType ?? ""}
            onValueChange={(v) =>
              setValue("raceType", v as RaceTypeValue, { shouldValidate: true })
            }
          >
            <SelectTrigger
              id="raceType"
              aria-invalid={!!errors.raceType}
              className={cn(errors.raceType && "border-destructive")}
            >
              <SelectValue placeholder="Selecciona un tipo" />
            </SelectTrigger>
            <SelectContent>
              {RACE_TYPES.map((t) => (
                <SelectItem key={t} value={t}>
                  {RACE_TYPE_LABEL[t]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <FieldError id="raceType-error" message={errors.raceType?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="registrationDeadline">Cierre de inscripciones</Label>
          <Input
            id="registrationDeadline"
            type="date"
            min={today}
            aria-invalid={!!errors.registrationDeadline}
            aria-describedby={
              errors.registrationDeadline ? "registrationDeadline-error" : undefined
            }
            {...register("registrationDeadline")}
          />
          <FieldError
            id="registrationDeadline-error"
            message={errors.registrationDeadline?.message}
          />
        </div>
      </div>

      <p className="text-sm text-muted-foreground">
        La carrera se crea en estado borrador. El avance de estados se gestiona desde su ficha.
      </p>

      <div className="flex gap-3">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting && <Loader2 className="size-4 animate-spin" aria-hidden />}
          {submitLabel}
        </Button>
        <Button type="button" variant="outline" asChild>
          <Link to={cancelTo}>Cancelar</Link>
        </Button>
      </div>
    </form>
  );
}