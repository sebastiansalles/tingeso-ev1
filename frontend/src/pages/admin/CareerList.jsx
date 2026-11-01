import { useEffect, useState } from "react";
import {
  Alert, Chip, CircularProgress, Paper, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import careerService from "../../services/career.service";

export default function CareerList() {
  const [careers, setCareers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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
      <Typography variant="h5" gutterBottom>Carreras</Typography>
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Código</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Descripción</TableCell>
              <TableCell>Estado</TableCell>
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
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}