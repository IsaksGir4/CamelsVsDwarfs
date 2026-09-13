import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import type Keycloak from "keycloak-js";
import { getKeycloak } from "./keycloak";

export type AppRole = "admin" | "organizer" | "viewer";

export type Permission =
  | "competitors:write"
  | "teams:write"
  | "races:write"
  | "registrations:manage"
  | "results:write"
  | "audit:read";

const ROLE_PERMISSIONS: Record<AppRole, Permission[]> = {
  admin: [
    "competitors:write",
    "teams:write",
    "races:write",
    "registrations:manage",
    "results:write",
    "audit:read",
  ],
  organizer: ["races:write", "registrations:manage", "results:write"],
  viewer: [],
};

export interface AuthUser {
  id: string;
  username: string;
  name: string;
  email: string;
  roles: AppRole[];
}

interface AuthState {
  status: "loading" | "authenticated" | "anonymous" | "error";
  user: AuthUser | null;
  error: string | null;
  login: () => void;
  logout: () => void;
  getToken: () => Promise<string | null>;
  hasRole: (role: AppRole) => boolean;
  hasAnyRole: (roles: AppRole[]) => boolean;
  hasPermission: (permission: Permission) => boolean;
}

const AuthContext = createContext<AuthState | null>(null);

const KNOWN_ROLES: AppRole[] = ["admin", "organizer", "viewer"];

function readUser(kc: Keycloak): AuthUser | null {
  const parsed = kc.tokenParsed as
    | {
        sub?: string;
        preferred_username?: string;
        name?: string;
        email?: string;
        realm_access?: { roles?: string[] };
      }
    | undefined;
  if (!parsed) return null;
  const roles = (parsed.realm_access?.roles ?? []).filter((r): r is AppRole =>
    (KNOWN_ROLES as string[]).includes(r),
  );
  return {
    id: parsed.sub ?? "",
    username: parsed.preferred_username ?? "",
    name: parsed.name ?? parsed.preferred_username ?? "Usuario",
    email: parsed.email ?? "",
    roles: roles.length > 0 ? roles : ["viewer"],
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthState["status"]>("loading");
  const [user, setUser] = useState<AuthUser | null>(null);
  const [error, setError] = useState<string | null>(null);
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const kc = getKeycloak();

    kc.onTokenExpired = () => {
      void kc.updateToken(30).catch(() => {
        setStatus("anonymous");
        setUser(null);
      });
    };
    kc.onAuthRefreshSuccess = () => setUser(readUser(kc));
    kc.onAuthLogout = () => {
      setUser(null);
      setStatus("anonymous");
    };

    kc.init({
      onLoad: "check-sso",
      pkceMethod: "S256",
      checkLoginIframe: false,
    })
      .then((authenticated) => {
        if (authenticated) {
          setUser(readUser(kc));
          setStatus("authenticated");
        } else {
          setStatus("anonymous");
        }
      })
      .catch((e: unknown) => {
        setError(
          e instanceof Error
            ? e.message
            : "No se pudo contactar con el servidor de autenticación.",
        );
        setStatus("error");
      });

    const interval = window.setInterval(() => {
      void kc.updateToken(60).catch(() => undefined);
    }, 30_000);
    return () => window.clearInterval(interval);
  }, []);

  const login = useCallback(() => {
    void getKeycloak().login({ redirectUri: window.location.origin + "/" });
  }, []);

  const logout = useCallback(() => {
    void getKeycloak().logout({ redirectUri: window.location.origin + "/login" });
  }, []);

  const getToken = useCallback(async () => {
    const kc = getKeycloak();
    if (!kc.authenticated) return null;
    try {
      await kc.updateToken(30);
    } catch {
      return null;
    }
    return kc.token ?? null;
  }, []);

  const value = useMemo<AuthState>(() => {
    const roles = user?.roles ?? [];
    const permissions = new Set(roles.flatMap((r) => ROLE_PERMISSIONS[r] ?? []));
    return {
      status,
      user,
      error,
      login,
      logout,
      getToken,
      hasRole: (role) => roles.includes(role),
      hasAnyRole: (list) => list.some((r) => roles.includes(r)),
      hasPermission: (permission) => permissions.has(permission),
    };
  }, [status, user, error, login, logout, getToken]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth debe usarse dentro de <AuthProvider>");
  return ctx;
}