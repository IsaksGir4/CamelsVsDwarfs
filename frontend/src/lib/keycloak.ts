import Keycloak from "keycloak-js";

export const keycloakConfig = {
  url: import.meta.env['VITE_KEYCLOAK_URL'] ?? "http://localhost:8081",
  realm: import.meta.env['VITE_KEYCLOAK_REALM'] ?? "camelsvsdwarfs",
  clientId: import.meta.env['VITE_KEYCLOAK_CLIENT_ID'] ?? "camelsvsdwarfs-frontend",
};

export const API_BASE_URL: string =
  import.meta.env['VITE_API_BASE_URL'] ?? "http://localhost:8080/api";

let instance: Keycloak | null = null;

export function getKeycloak(): Keycloak {
  if (!instance) {
    instance = new Keycloak(keycloakConfig);
  }
  return instance;
}
