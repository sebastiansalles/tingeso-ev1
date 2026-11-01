import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import keycloak from "./services/keycloak";

// Without a session, Keycloak redirects to its login page; React starts only after login
keycloak
  .init({ onLoad: "login-required", pkceMethod: "S256", checkLoginIframe: false })
  .then(() => {
    createRoot(document.getElementById("root")).render(
      <StrictMode>
        <App />
      </StrictMode>
    );
  })
  .catch(() => {
    document.getElementById("root").innerText = "Could not connect to the authentication server (Keycloak).";
  });