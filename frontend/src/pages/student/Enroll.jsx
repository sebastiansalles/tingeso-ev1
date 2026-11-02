import { useEffect, useState } from "react";
import {
  Alert, Button, Checkbox, Paper, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, MenuItem, Typography,
} from "@mui/material";
import studentService from "../../services/student.service";
import periodService from "../../services/period.service";
import sectionService from "../../services/section.service";
import enrollmentService from "../../services/enrollment.service";
import errorMessage from "../../utils/errorMessage";
import { blockCode } from "../../constants";
import ScheduleGrid from "../../components/ScheduleGrid";

// Enrollment: the student chooses subjects available in an open period
export default function Enroll() {
  const [student, setStudent] = useState(null);
  const [periods, setPeriods] = useState([]);
  const [periodId, setPeriodId] = useState("");
  const [available, setAvailable] = useState([]);   // subjects the student can enroll now
  const [enrolled, setEnrolled] = useState([]);     // enrollments already made in this period
  const [schedules, setSchedules] = useState({});   // subjectId -> ["L1", "W1"]
  const [selected, setSelected] = useState([]);     // ids of the chosen subjects
  const [version, setVersion] = useState(0);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // On entering: who am I, and the open periods (the only ones that accept enrollments)
  useEffect(() => {
    Promise.all([studentService.getMe(), periodService.getAll()])
      .then(([me, periodsResponse]) => {
        setStudent(me.data);
        const open = periodsResponse.data.filter((p) => p.status === "OPEN");
        setPeriods(open);
        if (open.length > 0) setPeriodId(open[0].id);
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  // When the period changes: available subjects, current enrollments and the schedule of every section
  useEffect(() => {
    if (!student || !periodId) return;
    Promise.all([
      enrollmentService.getAvailableSubjects(student.id, periodId),
      enrollmentService.getEnrollments(student.id, periodId),
      sectionService.getByPeriod(periodId),
    ])
      .then(([availableResponse, enrolledResponse, sectionsResponse]) => {
        const sections = sectionsResponse.data;
        return Promise.all(sections.map((s) => sectionService.getSchedule(s.id))).then((answers) => {
          const map = {};
          sections.forEach((s, i) => { map[s.subjectId] = answers[i].data.map(blockCode); });
          setAvailable([...availableResponse.data].sort((a, b) => a.semester - b.semester || a.code.localeCompare(b.code)));
          setEnrolled(enrolledResponse.data);
          setSchedules(map);
          setSelected([]);
        });
      })
      .catch((err) => setError(errorMessage(err)));
  }, [student, periodId, version]);

  const toggle = (subjectId) => {
    setSelected((previous) => previous.includes(subjectId)
      ? previous.filter((id) => id !== subjectId)
      : [...previous, subjectId]);
  };

  // Preview of the week: subjects already enrolled plus the ones being chosen
  const cells = {};
  enrolled.forEach((e) => (schedules[e.subjectId] ?? []).forEach((code) => { cells[code] = e.subjectCode; }));
  available.filter((s) => selected.includes(s.id))
    .forEach((s) => (schedules[s.id] ?? []).forEach((code) => {
      cells[code] = cells[code] ? `${cells[code]} / ${s.code} ⚠` : s.code;   // ⚠ = schedule conflict
    }));

  const total = enrolled.length + selected.length;

  const handleEnroll = () => {
    setError(null);
    setSuccess(null);
    enrollmentService.enroll(student.id, periodId, selected)
      .then((response) => {
        setSuccess(`Inscribiste ${response.data.length} asignatura(s).`);
        setVersion((v) => v + 1);
      })
      .catch((err) => setError(errorMessage(err)));
  };

  if (!student) return error ? <Alert severity="error">{error}</Alert> : null;

  return (
    <>
      <Typography variant="h5" gutterBottom>Inscripción de asignaturas</Typography>
      {student.status !== "REGULAR" && (
        <Alert severity="warning" sx={{ mb: 2 }}>Solo los estudiantes en estado regular pueden inscribir asignaturas.</Alert>
      )}
      {periods.length === 0 && <Alert severity="info" sx={{ mb: 2 }}>No hay períodos abiertos para inscripción.</Alert>}
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>{success}</Alert>}

      {periods.length > 0 && (
        <TextField select label="Período" value={periodId} onChange={(e) => setPeriodId(e.target.value)} sx={{ minWidth: 200, mb: 2 }}>
          {periods.map((p) => <MenuItem key={p.id} value={p.id}>{p.year}-{p.semester}</MenuItem>)}
        </TextField>
      )}

      <Typography variant="subtitle1" gutterBottom>Asignaturas disponibles</Typography>
      <TableContainer component={Paper} sx={{ mb: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox" />
              <TableCell>Sem.</TableCell>
              <TableCell>Código</TableCell>
              <TableCell>Asignatura</TableCell>
              <TableCell>SCT</TableCell>
              <TableCell>Horario</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {available.map((s) => (
              <TableRow key={s.id} hover onClick={() => toggle(s.id)} sx={{ cursor: "pointer" }}>
                <TableCell padding="checkbox"><Checkbox checked={selected.includes(s.id)} /></TableCell>
                <TableCell>{s.semester}</TableCell>
                <TableCell>{s.code}</TableCell>
                <TableCell>{s.name}</TableCell>
                <TableCell>{s.sct}</TableCell>
                <TableCell>{(schedules[s.id] ?? []).join(", ")}</TableCell>
              </TableRow>
            ))}
            {available.length === 0 && (
              <TableRow><TableCell colSpan={6}>No tienes asignaturas disponibles en este período.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Stack direction="row" spacing={2} sx={{ alignItems: "center", mb: 2 }}>
        <Button variant="contained" disabled={selected.length === 0} onClick={handleEnroll}>
          Inscribir {selected.length > 0 ? `(${selected.length})` : ""}
        </Button>
        <Typography variant="body2" color={total > 6 ? "error" : "text.secondary"}>
          Ya inscritas: {enrolled.length} · Total del período: {total} (mínimo 3, máximo 6)
        </Typography>
      </Stack>

      <Typography variant="subtitle1" gutterBottom>Vista previa del horario</Typography>
      <ScheduleGrid cells={cells} />
    </>
  );
}