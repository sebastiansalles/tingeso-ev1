import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Alert, Button, Chip, CircularProgress, IconButton, Paper, Stack, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import studentService from "../../services/student.service";
import careerService from "../../services/career.service";
import { STUDENT_STATUS } from "../../constants";

const STATUS_COLOR = { REGULAR: "success", GRADUATED: "primary", ELIMINATED: "error" };

export default function StudentList() {
  const [students, setStudents] = useState([]);
  const [careers, setCareers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  // Students and careers are loaded together: careers are needed to show the career code
  useEffect(() => {
    Promise.all([studentService.getAll(), careerService.getAll()])
      .then(([studentsResponse, careersResponse]) => {
        setStudents(studentsResponse.data);
        setCareers(careersResponse.data);
      })
      .catch(() => setError("No se pudieron cargar los estudiantes."))
      .finally(() => setLoading(false));
  }, []);

  const careerCode = (careerId) => careers.find((c) => c.id === careerId)?.code ?? careerId;

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Estudiantes</Typography>
        <Button variant="contained" onClick={() => navigate("/students/new")}>Nuevo estudiante</Button>
      </Stack>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>RUN</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Correo</TableCell>
              <TableCell>Carrera</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {students.map((student) => (
              <TableRow key={student.id}>
                <TableCell>{student.run}</TableCell>
                <TableCell>{student.fullName}</TableCell>
                <TableCell>{student.email}</TableCell>
                <TableCell>{careerCode(student.careerId)}</TableCell>
                <TableCell>
                  <Chip label={STUDENT_STATUS[student.status]} color={STATUS_COLOR[student.status] ?? "default"} size="small" />
                </TableCell>
                <TableCell align="right">
                  <IconButton onClick={() => navigate(`/students/${student.id}/edit`)} aria-label="Editar">
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