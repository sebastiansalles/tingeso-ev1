import keycloak from "./services/keycloak";

export const ROLES = { ADMIN: "ADMIN", TEACHER: "TEACHER", STUDENT: "STUDENT" };

// Roles come from the Keycloak token, the same claim the backend reads
export function hasRole(role) {
  return keycloak.tokenParsed?.realm_access?.roles?.includes(role) ?? false;
}

export function currentUser() {
  return {
    username: keycloak.tokenParsed?.preferred_username,
    name: keycloak.tokenParsed?.name,
    email: keycloak.tokenParsed?.email,
  };
}