import { useEffect, useState } from "react";
import {
  Alert, Chip, CircularProgress, Paper, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import studentService from "../../services/student.service";
import enrollmentService from "../../services/enrollment.service";
import periodService from "../../services/period.service";
import errorMessage from "../../utils/errorMessage";

// Academic history: subjects of closed periods, with final grade and result
export default function History() {
  const [history, setHistory] = useState([]);
  const [periods, setPeriods] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    studentService.getMe()
      .then((me) => Promise.all([enrollmentService.getAcademicHistory(me.data.id), periodService.getAll()]))
      .then(([historyResponse, periodsResponse]) => {
        setHistory(historyResponse.data);
        setPeriods(periodsResponse.data);
      })
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;

  const periodOf = (id) => periods.find((p) => p.id === id);
  const periodName = (id) => { const p = periodOf(id); return p ? `${p.year}-${p.semester}` : id; };
  // Oldest period first, like an official transcript
  const sorted = [...history].sort((a, b) => {
    const pa = periodOf(a.academicPeriodId);
    const pb = periodOf(b.academicPeriodId);
    return (pa?.year ?? 0) - (pb?.year ?? 0) || (pa?.semester ?? 0) - (pb?.semester ?? 0)
      || a.subjectCode.localeCompare(b.subjectCode);
  });

  return (
    <>
      <Typography variant="h5" gutterBottom>Historial académico</Typography>
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Período</TableCell>
              <TableCell>Código</TableCell>
              <TableCell>Asignatura</TableCell>
              <TableCell>SCT</TableCell>
              <TableCell>Nota final</TableCell>
              <TableCell>Resultado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {sorted.map((e) => (
              <TableRow key={e.id}>
                <TableCell>{periodName(e.academicPeriodId)}</TableCell>
                {/* Copy of the subject saved at enrollment time: later changes to the subject do not alter it */}
                <TableCell>{e.subjectCode}</TableCell>
                <TableCell>{e.subjectName}</TableCell>
                <TableCell>{e.subjectSct}</TableCell>
                <TableCell>{e.finalGrade?.toFixed(1)}</TableCell>
                <TableCell>
                  <Chip size="small" label={e.result === "APPROVED" ? "Aprobado" : "Reprobado"}
                        color={e.result === "APPROVED" ? "success" : "error"} />
                </TableCell>
              </TableRow>
            ))}
            {history.length === 0 && (
              <TableRow><TableCell colSpan={6}>Aún no tienes asignaturas en tu historial.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}