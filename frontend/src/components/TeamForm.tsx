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
import { TEAM_CATEGORY_LABEL, type TeamInput } from "@/lib/league-api";
import { cn } from "@/lib/utils";

const CATEGORIES = ["DUO", "TRIO", "QUARTET"] as const;
type CategoryValue = (typeof CATEGORIES)[number];

export const teamSchema = z.object({
  teamName: z
    .string()
    .trim()
    .min(3, "El nombre debe tener al menos 3 caracteres.")
    .max(100, "El nombre no puede superar 100 caracteres."),
  description: z
    .string()
    .trim()
    .min(1, "La descripcion es obligatoria.")
    .max(512, "La descripcion no puede superar 512 caracteres."),
  responsableCoach: z
    .string()
    .trim()
    .max(100, "El nombre del entrenador no puede superar 100 caracteres.")
    .default(""),
  category: z.enum(CATEGORIES, { message: "Selecciona una categoria." }),
});

export interface TeamFormValues {
  teamName: string;
  description: string;
  responsableCoach: string;
  category: CategoryValue;
}

function FieldError({ id, message }: { id: string; message?: string | undefined }) {
  if (!message) return null;
  return (
    <p id={id} role="alert" className="text-sm font-medium text-destructive">
      {message}
    </p>
  );
}

export function TeamForm({
  defaultValues,
  submitLabel,
  cancelTo,
  onSubmit,
  categoryHint,
}: {
  defaultValues?: Partial<TeamFormValues>;
  submitLabel: string;
  cancelTo: string;
  onSubmit: (values: TeamInput) => Promise<unknown>;
  categoryHint?: string;
}) {
  const form = useForm<TeamFormValues>({
    resolver: zodResolver(teamSchema) as unknown as Resolver<TeamFormValues>,
    mode: "onBlur",
    defaultValues: {
      teamName: "",
      description: "",
      responsableCoach: "",
      ...defaultValues,
    } as TeamFormValues,
  });

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = form;

  const category = watch("category");

  const submit = handleSubmit(async (values) => {
    try {
      await onSubmit({
        teamName: values.teamName,
        description: values.description,
        responsableCoach: values.responsableCoach,
        category: values.category,
      });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        // El backend devuelve 409 tanto por nombre duplicado como por reducir
        // la categoria por debajo de los miembros activos.
        const field = error.message.toLowerCase().includes("categoria")
          ? "category"
          : "teamName";
        setError(field, { type: "server", message: error.message }, { shouldFocus: true });
        return;
      }
      throw error;
    }
  });

  return (
    <form onSubmit={submit} noValidate className="max-w-2xl space-y-6">
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="teamName">Nombre del equipo</Label>
          <Input
            id="teamName"
            autoComplete="off"
            aria-invalid={!!errors.teamName}
            aria-describedby={errors.teamName ? "teamName-error" : undefined}
            {...register("teamName")}
          />
          <FieldError id="teamName-error" message={errors.teamName?.message} />
        </div>

        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="description">Descripción</Label>
          <Input
            id="description"
            autoComplete="off"
            aria-invalid={!!errors.description}
            aria-describedby={errors.description ? "description-error" : undefined}
            {...register("description")}
          />
          <FieldError id="description-error" message={errors.description?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="responsableCoach">Entrenador o responsable</Label>
          <Input
            id="responsableCoach"
            autoComplete="off"
            placeholder="Opcional"
            {...register("responsableCoach")}
          />
          <FieldError id="responsableCoach-error" message={errors.responsableCoach?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="category">Categoría</Label>
          <Select
            value={category ?? ""}
            onValueChange={(v) =>
              setValue("category", v as CategoryValue, { shouldValidate: true })
            }
          >
            <SelectTrigger
              id="category"
              aria-invalid={!!errors.category}
              className={cn(errors.category && "border-destructive")}
            >
              <SelectValue placeholder="Selecciona una categoría" />
            </SelectTrigger>
            <SelectContent>
              {CATEGORIES.map((c) => (
                <SelectItem key={c} value={c}>
                  {TEAM_CATEGORY_LABEL[c]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <FieldError id="category-error" message={errors.category?.message} />
          <p className="text-xs text-muted-foreground">
            {categoryHint ?? "La categoría determina el máximo de miembros del equipo."}
          </p>
        </div>
      </div>

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