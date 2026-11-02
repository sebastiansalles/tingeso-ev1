import { useEffect, useState } from "react";
import {
  Alert, Button, MenuItem, Paper, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, Typography,
} from "@mui/material";
import periodService from "../../services/period.service";
import sectionService from "../../services/section.service";
import subjectService from "../../services/subject.service";
import teacherService from "../../services/teacher.service";
import errorMessage from "../../utils/errorMessage";
import { blockCode } from "../../constants";
import SectionDialog from "./SectionDialog";

// Academic offer of a period: one section per subject, with teacher, capacity and schedule
export default function SectionList() {
  const [periods, setPeriods] = useState([]);
  const [periodId, setPeriodId] = useState("");
  const [sections, setSections] = useState([]);
  const [schedules, setSchedules] = useState({});   // sectionId -> ["L1", "W1", ...]
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [version, setVersion] = useState(0);
  const [error, setError] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  // On entering: periods (most recent first), subjects and teachers to show names
  useEffect(() => {
    Promise.all([periodService.getAll(), subjectService.getAll(), teacherService.getAll()])
      .then(([periodsResponse, subjectsResponse, teachersResponse]) => {
        const sorted = [...periodsResponse.data].sort((a, b) => b.year - a.year || b.semester - a.semester);
        setPeriods(sorted);
        setSubjects(subjectsResponse.data);
        setTeachers(teachersResponse.data);
        const open = sorted.find((p) => p.status === "OPEN") ?? sorted[0];
        if (open) setPeriodId(open.id);
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  // When the period changes: its sections and, in parallel, the schedule of each one
  useEffect(() => {
    if (!periodId) return;
    sectionService.getByPeriod(periodId)
      .then((response) => {
        const list = response.data;
        return Promise.all(list.map((s) => sectionService.getSchedule(s.id)))
          .then((answers) => {
            const map = {};
            list.forEach((s, i) => { map[s.id] = answers[i].data.map(blockCode); });
            setSections(list);
            setSchedules(map);
          });
      })
      .catch((err) => setError(errorMessage(err)));
  }, [periodId, version]);

  const period = periods.find((p) => p.id === periodId);
  const subjectOf = (id) => subjects.find((s) => s.id === id);
  const teacherName = (id) => teachers.find((t) => t.id === id)?.fullName ?? id;
  const sortedSections = [...sections].sort((a, b) =>
    (subjectOf(a.subjectId)?.code ?? "").localeCompare(subjectOf(b.subjectId)?.code ?? ""));

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Oferta académica</Typography>
        <Button variant="contained" disabled={period?.status !== "OPEN"} onClick={() => setDialogOpen(true)}>
          Nueva sección
        </Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <TextField select label="Período" value={periodId} onChange={(e) => setPeriodId(e.target.value)}
                 sx={{ minWidth: 220, mb: 2 }}>
        {periods.map((p) => (
          <MenuItem key={p.id} value={p.id}>{p.year}-{p.semester} {p.status === "OPEN" ? "(abierto)" : "(cerrado)"}</MenuItem>
        ))}
      </TextField>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Código</TableCell>
              <TableCell>Asignatura</TableCell>
              <TableCell>Docente</TableCell>
              <TableCell>Cupos</TableCell>
              <TableCell>Horario</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {sortedSections.map((section) => (
              <TableRow key={section.id}>
                <TableCell>{subjectOf(section.subjectId)?.code}</TableCell>
                <TableCell>{subjectOf(section.subjectId)?.name}</TableCell>
                <TableCell>{teacherName(section.teacherId)}</TableCell>
                <TableCell>{section.capacity}</TableCell>
                <TableCell>{(schedules[section.id] ?? []).join(", ")}</TableCell>
              </TableRow>
            ))}
            {sections.length === 0 && (
              <TableRow><TableCell colSpan={5}>Este período no tiene secciones.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {dialogOpen && period && (
        <SectionDialog period={period} offeredSubjectIds={sections.map((s) => s.subjectId)}
                       onClose={() => setDialogOpen(false)}
                       onSaved={() => { setDialogOpen(false); setVersion((v) => v + 1); }} />
      )}
    </>
  );
}