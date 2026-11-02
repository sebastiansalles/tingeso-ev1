import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Alert, Button, Chip, CircularProgress, Paper, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, Typography,
} from "@mui/material";
import sectionService from "../../services/section.service";
import subjectService from "../../services/subject.service";
import periodService from "../../services/period.service";
import gradeService from "../../services/grade.service";
import errorMessage from "../../utils/errorMessage";

// Students of one section and their final grades
export default function SectionGrades() {
  const { sectionId } = useParams();
  const navigate = useNavigate();

  const [subject, setSubject] = useState(null);
  const [period, setPeriod] = useState(null);
  const [enrollments, setEnrollments] = useState([]);
  const [students, setStudents] = useState([]);
  const [drafts, setDrafts] = useState({});   // enrollmentId -> grade being typed
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    sectionService.get(sectionId)
      .then((section) => Promise.all([
        subjectService.get(section.data.subjectId),
        periodService.get(section.data.academicPeriodId),
        gradeService.getSectionEnrollments(sectionId),
        gradeService.getSectionStudents(sectionId),
      ]))
      .then(([subjectResponse, periodResponse, enrollmentsResponse, studentsResponse]) => {
        setSubject(subjectResponse.data);
        setPeriod(periodResponse.data);
        setEnrollments(enrollmentsResponse.data);
        setStudents(studentsResponse.data);
      })
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, [sectionId]);

  const studentOf = (id) => students.find((s) => s.id === id);
  const isOpen = period?.status === "OPEN";
  // What the input shows: what is being typed, or else the saved grade
  const valueOf = (enrollment) => drafts[enrollment.id] ?? enrollment.finalGrade ?? "";

  const handleSave = (enrollment) => {
    setError(null);
    setSuccess(null);
    gradeService.registerGrade(enrollment.id, Number(valueOf(enrollment)))
      .then((response) => {
        // Replace the saved enrollment (now with grade and result) and clear its draft
        setEnrollments((previous) => previous.map((e) => (e.id === enrollment.id ? response.data : e)));
        setDrafts((previous) => { const next = { ...previous }; delete next[enrollment.id]; return next; });
        setSuccess(`Nota de ${studentOf(enrollment.studentId)?.fullName ?? "estudiante"} guardada.`);
      })
      .catch((err) => setError(errorMessage(err)));
  };

  if (loading) return <CircularProgress />;

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">
          {subject ? `${subject.code} - ${subject.name}` : "Sección"} {period && `(${period.year}-${period.semester})`}
        </Typography>
        <Button onClick={() => navigate("/my-sections")}>Volver</Button>
      </Stack>

      {!isOpen && period && (
        <Alert severity="info" sx={{ mb: 2 }}>El período está cerrado: las notas ya forman parte del historial y no se pueden modificar.</Alert>
      )}
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>{success}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>RUN</TableCell>
              <TableCell>Estudiante</TableCell>
              <TableCell>Nota final</TableCell>
              <TableCell>Resultado</TableCell>
              {isOpen && <TableCell align="right">Acciones</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {enrollments.map((enrollment) => (
              <TableRow key={enrollment.id}>
                <TableCell>{studentOf(enrollment.studentId)?.run}</TableCell>
                <TableCell>{studentOf(enrollment.studentId)?.fullName}</TableCell>
                <TableCell>
                  {isOpen ? (
                    <TextField type="number" size="small" value={valueOf(enrollment)} sx={{ width: 100 }}
                               onChange={(e) => setDrafts((previous) => ({ ...previous, [enrollment.id]: e.target.value }))}
                               slotProps={{ htmlInput: { min: 1, max: 7, step: 0.1 } }} />
                  ) : (enrollment.finalGrade ?? "—")}
                </TableCell>
                <TableCell>
                  {enrollment.result
                    ? <Chip size="small" label={enrollment.result === "APPROVED" ? "Aprobado" : "Reprobado"}
                            color={enrollment.result === "APPROVED" ? "success" : "error"} />
                    : <Chip size="small" label="Pendiente" />}
                </TableCell>
                {isOpen && (
                  <TableCell align="right">
                    <Button size="small" variant="contained" disabled={valueOf(enrollment) === ""}
                            onClick={() => handleSave(enrollment)}>
                      Guardar
                    </Button>
                  </TableCell>
                )}
              </TableRow>
            ))}
            {enrollments.length === 0 && (
              <TableRow><TableCell colSpan={5}>No hay estudiantes inscritos en esta sección.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}