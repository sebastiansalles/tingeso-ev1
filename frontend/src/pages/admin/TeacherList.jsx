import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Alert, Button, Chip, CircularProgress, IconButton, Paper, Stack, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import teacherService from "../../services/teacher.service";
import { TEACHER_STATUS } from "../../constants";

export default function TeacherList() {
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    teacherService.getAll()
      .then((response) => setTeachers(response.data))
      .catch(() => setError("No se pudieron cargar los docentes."))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Docentes</Typography>
        <Button variant="contained" onClick={() => navigate("/teachers/new")}>Nuevo docente</Button>
      </Stack>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>RUN</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Correo</TableCell>
              <TableCell>Título profesional</TableCell>
              <TableCell>Grado académico</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {teachers.map((teacher) => (
              <TableRow key={teacher.id}>
                <TableCell>{teacher.run}</TableCell>
                <TableCell>{teacher.fullName}</TableCell>
                <TableCell>{teacher.email}</TableCell>
                <TableCell>{teacher.professionalTitle}</TableCell>
                <TableCell>{teacher.academicDegree}</TableCell>
                <TableCell>
                  <Chip label={TEACHER_STATUS[teacher.status]} size="small"
                        color={teacher.status === "ACTIVE" ? "success" : "default"} />
                </TableCell>
                <TableCell align="right">
                  <IconButton onClick={() => navigate(`/teachers/${teacher.id}/edit`)} aria-label="Editar">
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