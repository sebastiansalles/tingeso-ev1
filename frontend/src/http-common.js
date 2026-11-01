import axios from "axios";
import keycloak from "./services/keycloak";

const api = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_URL,
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