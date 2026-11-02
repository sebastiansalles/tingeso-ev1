import Keycloak from "keycloak-js";

// Same idea as the backend URL: in the Docker image, Keycloak is on the same server, port 9090
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || `${window.location.protocol}//${window.location.hostname}:9090`,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

export default keycloak;