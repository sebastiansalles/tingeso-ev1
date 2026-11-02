import axios from "axios";
import keycloak from "./services/keycloak";


// In development it comes from .env.development. In the Docker image it is not defined, so the browser
// uses the same server it loaded the page from, on port 8080 (the Nginx load balancer)
const backendUrl = import.meta.env.VITE_BACKEND_URL
  || `${window.location.protocol}//${window.location.hostname}:8080/api/v1`;

const api = axios.create({
  baseURL: backendUrl,
  headers: { "Content-Type": "application/json" },
});

// Every request carries the Keycloak token; it is refreshed if it expires in less than 30 s
api.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    await keycloak.updateToken(30);
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

export default api;