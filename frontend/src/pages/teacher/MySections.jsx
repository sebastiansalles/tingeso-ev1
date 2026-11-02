import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Alert, Button, Chip, CircularProgress, Paper, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import teacherService from "../../services/teacher.service";
import periodService from "../../services/period.service";
import subjectService from "../../services/subject.service";
import errorMessage from "../../utils/errorMessage";

// Sections assigned to the logged-in teacher, in every period
export default function MySections() {
  const [sections, setSections] = useState([]);
  const [periods, setPeriods] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    // First "who am I" (the teacher linked to this Keycloak account), then their sections
    teacherService.getMe()
      .then((me) => Promise.all([
        teacherService.getSections(me.data.id), periodService.getAll(), subjectService.getAll(),
      ]))
      .then(([sectionsResponse, periodsResponse, subjectsResponse]) => {
        setSections(sectionsResponse.data);
        setPeriods(periodsResponse.data);
        setSubjects(subjectsResponse.data);
      })
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const periodOf = (id) => periods.find((p) => p.id === id);
  const subjectOf = (id) => subjects.find((s) => s.id === id);

  // Open periods first, then the most recent
  const sorted = [...sections].sort((a, b) => {
    const pa = periodOf(a.academicPeriodId);
    const pb = periodOf(b.academicPeriodId);
    return (pa?.status === "OPEN" ? 0 : 1) - (pb?.status === "OPEN" ? 0 : 1)
      || (pb?.year ?? 0) - (pa?.year ?? 0) || (pb?.semester ?? 0) - (pa?.semester ?? 0);
  });

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;

  return (
    <>
      <Typography variant="h5" gutterBottom>Mis secciones</Typography>
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Período</TableCell>
              <TableCell>Código</TableCell>
              <TableCell>Asignatura</TableCell>
              <TableCell>Cupos</TableCell>
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {sorted.map((section) => {
              const period = periodOf(section.academicPeriodId);
              return (
                <TableRow key={section.id}>
                  <TableCell>
                    {period?.year}-{period?.semester}{" "}
                    <Chip size="small" label={period?.status === "OPEN" ? "Abierto" : "Cerrado"}
                          color={period?.status === "OPEN" ? "success" : "default"} />
                  </TableCell>
                  <TableCell>{subjectOf(section.subjectId)?.code}</TableCell>
                  <TableCell>{subjectOf(section.subjectId)?.name}</TableCell>
                  <TableCell>{section.capacity}</TableCell>
                  <TableCell align="right">
                    <Button size="small" onClick={() => navigate(`/my-sections/${section.id}`)}>
                      Estudiantes y notas
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
            {sections.length === 0 && (
              <TableRow><TableCell colSpan={5}>No tienes secciones asignadas.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}