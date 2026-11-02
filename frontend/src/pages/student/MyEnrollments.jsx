import { useEffect, useState } from "react";
import {
  Alert, Button, Chip, MenuItem, Paper, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, Typography,
} from "@mui/material";
import studentService from "../../services/student.service";
import periodService from "../../services/period.service";
import sectionService from "../../services/section.service";
import enrollmentService from "../../services/enrollment.service";
import errorMessage from "../../utils/errorMessage";
import { blockCode } from "../../constants";
import ScheduleGrid from "../../components/ScheduleGrid";

// Enrollments of the logged-in student in a period, with their weekly schedule
export default function MyEnrollments() {
  const [student, setStudent] = useState(null);
  const [periods, setPeriods] = useState([]);
  const [periodId, setPeriodId] = useState("");
  const [enrollments, setEnrollments] = useState([]);
  const [schedules, setSchedules] = useState({});   // sectionId -> ["L1", "W1"]
  const [version, setVersion] = useState(0);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([studentService.getMe(), periodService.getAll()])
      .then(([me, periodsResponse]) => {
        setStudent(me.data);
        const sorted = [...periodsResponse.data].sort((a, b) => b.year - a.year || b.semester - a.semester);
        setPeriods(sorted);
        const open = sorted.find((p) => p.status === "OPEN") ?? sorted[0];
        if (open) setPeriodId(open.id);
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  useEffect(() => {
    if (!student || !periodId) return;
    enrollmentService.getEnrollments(student.id, periodId)
      .then((response) => {
        const list = response.data;
        return Promise.all(list.map((e) => sectionService.getSchedule(e.sectionId))).then((answers) => {
          const map = {};
          list.forEach((e, i) => { map[e.sectionId] = answers[i].data.map(blockCode); });
          setEnrollments(list);
          setSchedules(map);
        });
      })
      .catch((err) => setError(errorMessage(err)));
  }, [student, periodId, version]);

  const period = periods.find((p) => p.id === periodId);
  const isOpen = period?.status === "OPEN";

  const handleCancel = (enrollment) => {
    if (!window.confirm(`¿Anular la inscripción de ${enrollment.subjectCode}? El cupo quedará disponible.`)) return;
    setError(null);
    enrollmentService.cancel(enrollment.id)
      .then(() => setVersion((v) => v + 1))
      .catch((err) => setError(errorMessage(err)));
  };

  // Weekly schedule: each block shows the code of the subject
  const cells = {};
  enrollments.forEach((e) => (schedules[e.sectionId] ?? []).forEach((code) => { cells[code] = e.subjectCode; }));

  return (
    <>
      <Typography variant="h5" gutterBottom>Mis inscripciones</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <TextField select label="Período" value={periodId} onChange={(e) => setPeriodId(e.target.value)} sx={{ minWidth: 220, mb: 2 }}>
        {periods.map((p) => (
          <MenuItem key={p.id} value={p.id}>{p.year}-{p.semester} {p.status === "OPEN" ? "(abierto)" : "(cerrado)"}</MenuItem>
        ))}
      </TextField>

      <TableContainer component={Paper} sx={{ mb: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Código</TableCell>
              <TableCell>Asignatura</TableCell>
              <TableCell>SCT</TableCell>
              <TableCell>Horario</TableCell>
              <TableCell>Fecha de inscripción</TableCell>
              <TableCell>Nota</TableCell>
              {isOpen && <TableCell align="right">Acciones</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {enrollments.map((e) => (
              <TableRow key={e.id}>
                <TableCell>{e.subjectCode}</TableCell>
                <TableCell>{e.subjectName}</TableCell>
                <TableCell>{e.subjectSct}</TableCell>
                <TableCell>{(schedules[e.sectionId] ?? []).join(", ")}</TableCell>
                <TableCell>{e.enrollmentDate}</TableCell>
                <TableCell>
                  {e.finalGrade != null
                    ? <Chip size="small" label={e.finalGrade.toFixed(1)} color={e.result === "APPROVED" ? "success" : "error"} />
                    : "—"}
                </TableCell>
                {isOpen && (
                  <TableCell align="right">
                    {e.finalGrade == null && (
                      <Button size="small" color="error" onClick={() => handleCancel(e)}>Anular</Button>
                    )}
                  </TableCell>
                )}
              </TableRow>
            ))}
            {enrollments.length === 0 && (
              <TableRow><TableCell colSpan={7}>No tienes inscripciones en este período.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Typography variant="subtitle1" gutterBottom>Horario semanal</Typography>
      <ScheduleGrid cells={cells} />
    </>
  );
}