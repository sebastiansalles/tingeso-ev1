import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Alert, Button, Chip, CircularProgress, IconButton, Paper, Stack, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import careerService from "../../services/career.service";

export default function CareerList() {
  const [careers, setCareers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  // Runs once, when the page is shown: loads the careers from the backend
  useEffect(() => {
    careerService.getAll()
      .then((response) => setCareers(response.data))
      .catch(() => setError("No se pudieron cargar las carreras."))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Carreras</Typography>
        <Button variant="contained" onClick={() => navigate("/careers/new")}>Nueva carrera</Button>
      </Stack>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Código</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Descripción</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {careers.map((career) => (
              <TableRow key={career.id}>
                <TableCell>{career.code}</TableCell>
                <TableCell>{career.name}</TableCell>
                <TableCell>{career.description}</TableCell>
                <TableCell>
                  <Chip
                    label={career.status === "ACTIVE" ? "Activa" : "Inactiva"}
                    color={career.status === "ACTIVE" ? "success" : "default"}
                    size="small"
                  />
                </TableCell>
                <TableCell align="right">
                  <IconButton onClick={() => navigate(`/careers/${career.id}/edit`)} aria-label="Editar">
                    <EditIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}