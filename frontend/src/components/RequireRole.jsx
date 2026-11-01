import { Alert } from "@mui/material";
import { hasRole } from "../auth";

// Shows the page only if the user has one of the allowed roles
export default function RequireRole({ roles, children }) {
  if (!roles.some(hasRole)) {
    return <Alert severity="warning">No tienes permiso para ver esta página.</Alert>;
  }
  return children;
}