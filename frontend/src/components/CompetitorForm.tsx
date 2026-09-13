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
import { COMPETITOR_TYPE_LABEL, type CompetitorInput } from "@/lib/league-api";
import { cn } from "@/lib/utils";

const PLAYER_TYPES = ["DWARF", "CAMEL", "MEDIUM", "OTHER"] as const;
type PlayerType = (typeof PLAYER_TYPES)[number];

const decimalField = (label: string) =>
  z
    .string()
    .min(1, `${label} es obligatorio.`)
    .refine((v) => {
      const n = Number(v.replace(",", "."));
      return Number.isFinite(n) && n > 0;
    }, `${label} debe ser un numero positivo.`);

export const competitorSchema = z.object({
  name: z
    .string()
    .trim()
    .min(5, "El nombre debe tener al menos 5 caracteres.")
    .max(100, "El nombre no puede superar 100 caracteres."),
  nickname: z
    .string()
    .trim()
    .min(1, "El apodo es obligatorio.")
    .max(50, "El apodo no puede superar 50 caracteres."),
  playerType: z.enum(PLAYER_TYPES, {
    message: "Selecciona el tipo de competidor.",
  }),
  birthDate: z
    .string()
    .min(1, "La fecha de nacimiento es obligatoria.")
    .refine((v) => !Number.isNaN(new Date(v).getTime()), "Fecha no valida.")
    .refine((v) => new Date(v) < new Date(), "La fecha de nacimiento debe ser pasada."),
  height: decimalField("La altura"),
  weight: decimalField("El peso"),
  placeOfBirth: z
    .string()
    .trim()
    .min(1, "El lugar de origen es obligatorio.")
    .max(120, "El lugar de origen no puede superar 120 caracteres."),
});

export interface CompetitorFormValues {
  name: string;
  nickname: string;
  playerType: PlayerType;
  birthDate: string;
  height: string;
  weight: string;
  placeOfBirth: string;
}

function FieldError({ id, message }: { id: string; message?: string | undefined }) {
  if (!message) return null;
  return (
    <p id={id} role="alert" className="text-sm font-medium text-destructive">
      {message}
    </p>
  );
}

export function CompetitorForm({
  defaultValues,
  submitLabel,
  cancelTo,
  onSubmit,
}: {
  defaultValues?: Partial<CompetitorFormValues>;
  submitLabel: string;
  cancelTo: string;
  onSubmit: (values: CompetitorInput) => Promise<unknown>;
}) {
  const form = useForm<CompetitorFormValues>({
    resolver: zodResolver(competitorSchema) as unknown as Resolver<CompetitorFormValues>,
    mode: "onBlur",
    defaultValues: {
      name: "",
      nickname: "",
      birthDate: "",
      placeOfBirth: "",
      height: "",
      weight: "",
      ...defaultValues,
    } as CompetitorFormValues,
  });

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = form;

  const playerType = watch("playerType");

  const submit = handleSubmit(async (values) => {
    try {
      await onSubmit({
        name: values.name,
        nickname: values.nickname,
        playerType: values.playerType,
        birthDate: values.birthDate,
        height: Number(values.height.replace(",", ".")),
        weight: Number(values.weight.replace(",", ".")),
        placeOfBirth: values.placeOfBirth,
      });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setError("nickname", { type: "server", message: error.message }, { shouldFocus: true });
        return;
      }
      throw error;
    }
  });

  return (
    <form onSubmit={submit} noValidate className="max-w-2xl space-y-6">
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="name">Nombre</Label>
          <Input
            id="name"
            autoComplete="off"
            aria-invalid={!!errors.name}
            aria-describedby={errors.name ? "name-error" : undefined}
            {...register("name")}
          />
          <FieldError id="name-error" message={errors.name?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="nickname">Apodo</Label>
          <Input
            id="nickname"
            autoComplete="off"
            aria-invalid={!!errors.nickname}
            aria-describedby={errors.nickname ? "nickname-error" : undefined}
            {...register("nickname")}
          />
          <FieldError id="nickname-error" message={errors.nickname?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="playerType">Tipo</Label>
          <Select
            value={playerType ?? ""}
            onValueChange={(v) =>
              setValue("playerType", v as PlayerType, { shouldValidate: true })
            }
          >
            <SelectTrigger
              id="playerType"
              aria-invalid={!!errors.playerType}
              aria-describedby={errors.playerType ? "playerType-error" : undefined}
              className={cn(errors.playerType && "border-destructive")}
            >
              <SelectValue placeholder="Selecciona un tipo" />
            </SelectTrigger>
            <SelectContent>
              {PLAYER_TYPES.map((t) => (
                <SelectItem key={t} value={t}>
                  {COMPETITOR_TYPE_LABEL[t]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <FieldError id="playerType-error" message={errors.playerType?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="birthDate">Fecha de nacimiento</Label>
          <Input
            id="birthDate"
            type="date"
            max={new Date().toISOString().slice(0, 10)}
            aria-invalid={!!errors.birthDate}
            aria-describedby={errors.birthDate ? "birthDate-error" : undefined}
            {...register("birthDate")}
          />
          <FieldError id="birthDate-error" message={errors.birthDate?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="placeOfBirth">Lugar de origen</Label>
          <Input
            id="placeOfBirth"
            autoComplete="off"
            aria-invalid={!!errors.placeOfBirth}
            aria-describedby={errors.placeOfBirth ? "placeOfBirth-error" : undefined}
            {...register("placeOfBirth")}
          />
          <FieldError id="placeOfBirth-error" message={errors.placeOfBirth?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="height">Altura (m)</Label>
          <Input
            id="height"
            type="number"
            step="0.01"
            min="0"
            inputMode="decimal"
            aria-invalid={!!errors.height}
            aria-describedby={errors.height ? "height-error" : undefined}
            {...register("height")}
          />
          <FieldError id="height-error" message={errors.height?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="weight">Peso (kg)</Label>
          <Input
            id="weight"
            type="number"
            step="0.1"
            min="0"
            inputMode="decimal"
            aria-invalid={!!errors.weight}
            aria-describedby={errors.weight ? "weight-error" : undefined}
            {...register("weight")}
          />
          <FieldError id="weight-error" message={errors.weight?.message} />
        </div>
      </div>

      <p className="text-sm text-muted-foreground">
        El estado y las estadísticas del competidor no se editan aquí: el estado se cambia desde su
        ficha y las estadísticas las calcula la liga.
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