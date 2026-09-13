import { getKeycloak, API_BASE_URL } from "./keycloak";

export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}

export class ApiError extends Error {
  status: number;
  body: ApiErrorBody | null;

  constructor(message: string, status: number, body: ApiErrorBody | null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

async function authHeader(): Promise<Record<string, string>> {
  const kc = getKeycloak();
  if (!kc.authenticated) return {};
  try {
    await kc.updateToken(30);
  } catch {
    kc.login();
    return {};
  }
  return kc.token ? { Authorization: `Bearer ${kc.token}` } : {};
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  for (const [k, v] of Object.entries(await authHeader())) headers.set(k, v);

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError(
      "No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.",
      0,
      null,
    );
  }

  if (response.status === 401) {
    getKeycloak().login();
    throw new ApiError("Tu sesión ha caducado. Vuelve a iniciar sesión.", 401, null);
  }

  if (!response.ok) {
    let body: ApiErrorBody | null = null;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = null;
    }
    throw new ApiError(
      body?.message ?? "Se ha producido un error inesperado.",
      response.status,
      body,
    );
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

function withBody(method: string, body?: unknown): RequestInit {
  return body === undefined ? { method } : { method, body: JSON.stringify(body) };
}

export const api = {
  get: <T>(path: string) => apiFetch<T>(path),
  post: <T>(path: string, body?: unknown) => apiFetch<T>(path, withBody("POST", body)),
  put: <T>(path: string, body?: unknown) => apiFetch<T>(path, withBody("PUT", body)),
  patch: <T>(path: string, body?: unknown) => apiFetch<T>(path, withBody("PATCH", body)),
  delete: <T>(path: string) => apiFetch<T>(path, { method: "DELETE" }),
};

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "Se ha producido un error inesperado.";
}