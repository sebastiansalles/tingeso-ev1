import { Typography } from "@mui/material";
import { currentUser, hasRole, ROLES } from "../auth";

export default function Home() {
  const user = currentUser();
  const description = hasRole(ROLES.ADMIN) ? "Administrador Académico"
    : hasRole(ROLES.TEACHER) ? "Docente"
    : hasRole(ROLES.STUDENT) ? "Estudiante" : "Sin rol asignado";

  return (
    <>
      <Typography variant="h4" gutterBottom>Bienvenido/a, {user.name || user.username}</Typography>
      <Typography color="text.secondary">{description}. Usa el menú superior para navegar.</Typography>
    </>
  );
}