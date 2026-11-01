import { AppBar, Toolbar, Typography, Button, Box } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";
import keycloak from "../services/keycloak";
import { hasRole, currentUser, ROLES } from "../auth";

// One entry per page; each user only sees the entries of their role
const MENU = [
  { label: "Carreras", path: "/careers", role: ROLES.ADMIN },
  { label: "Asignaturas", path: "/subjects", role: ROLES.ADMIN },
  { label: "Estudiantes", path: "/students", role: ROLES.ADMIN },
  { label: "Docentes", path: "/teachers", role: ROLES.ADMIN },
  { label: "Períodos", path: "/periods", role: ROLES.ADMIN },
  { label: "Oferta", path: "/sections", role: ROLES.ADMIN },
  { label: "Mis secciones", path: "/my-sections", role: ROLES.TEACHER },
  { label: "Mi plan", path: "/my-plan", role: ROLES.STUDENT },
  { label: "Inscripción", path: "/enroll", role: ROLES.STUDENT },
  { label: "Mis inscripciones", path: "/my-enrollments", role: ROLES.STUDENT },
  { label: "Historial", path: "/history", role: ROLES.STUDENT },
];

export default function Navbar() {
  return (
    <AppBar position="static">
      <Toolbar sx={{ gap: 1, flexWrap: "wrap" }}>
        <Typography variant="h6" component={RouterLink} to="/" sx={{ color: "inherit", textDecoration: "none", mr: 2 }}>
          SIGA IPT
        </Typography>
        {MENU.filter((item) => hasRole(item.role)).map((item) => (
          <Button key={item.path} color="inherit" component={RouterLink} to={item.path}>
            {item.label}
          </Button>
        ))}
        <Box sx={{ flexGrow: 1 }} />
        <Typography variant="body2">{currentUser().username}</Typography>
        <Button color="inherit" onClick={() => keycloak.logout({ redirectUri: window.location.origin })}>
          Salir
        </Button>
      </Toolbar>
    </AppBar>
  );
}