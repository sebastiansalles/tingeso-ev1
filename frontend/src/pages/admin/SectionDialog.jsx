import { useEffect, useState } from "react";
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography,
} from "@mui/material";
import sectionService from "../../services/section.service";
import subjectService from "../../services/subject.service";
import teacherService from "../../services/teacher.service";
import errorMessage from "../../utils/errorMessage";
import ScheduleGrid from "../../components/ScheduleGrid";

// Creates the section of a subject in a period: teacher, capacity and weekly schedule
export default function SectionDialog({ period, offeredSubjectIds, onClose, onSaved }) {
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [form, setForm] = useState({ subjectId: "", teacherId: "", capacity: 40 });
  const [schedule, setSchedule] = useState([]);   // selected block codes: ["L1", "W1"]
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([subjectService.getAll(), teacherService.getAll()])
      .then(([subjectsResponse, teachersResponse]) => {
        setSubjects(subjectsResponse.data);
        setTeachers(teachersResponse.data);
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  // Only subjects without a section in this period, and only active teachers
  const subjectOptions = subjects
    .filter((s) => !offeredSubjectIds.includes(s.id))
    .sort((a, b) => a.code.localeCompare(b.code));
  const teacherOptions = teachers.filter((t) => t.status === "ACTIVE");

  // Number of blocks the subject needs: TEL / 2 (same rule the backend validates)
  const subject = subjects.find((s) => s.id === form.subjectId);
  const requiredBlocks = subject ? (subject.theoryHours + subject.exerciseHours + subject.labHours) / 2 : 0;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  };

  // Clicking a cell adds the block, or removes it if it was already chosen
  const toggleBlock = (code) => {
    setSchedule((previous) => previous.includes(code)
      ? previous.filter((c) => c !== code)
      : [...previous, code]);
  };

  const handleSave = () => {
    sectionService.create({
      subjectId: form.subjectId,
      academicPeriodId: period.id,
      teacherId: form.teacherId,
      capacity: Number(form.capacity),
      schedule,
    })
      .then(() => onSaved())
      .catch((err) => setError(errorMessage(err)));
  };

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>Nueva sección · {period.year}-{period.semester}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Stack direction="row" spacing={2}>
            <TextField select label="Asignatura" name="subjectId" value={form.subjectId} onChange={handleChange} sx={{ flex: 2 }}>
              {subjectOptions.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} - {s.name}</MenuItem>)}
            </TextField>
            <TextField select label="Docente" name="teacherId" value={form.teacherId} onChange={handleChange} sx={{ flex: 2 }}>
              {teacherOptions.map((t) => <MenuItem key={t.id} value={t.id}>{t.fullName}</MenuItem>)}
            </TextField>
            <TextField type="number" label="Cupos" name="capacity" value={form.capacity} onChange={handleChange}
                       slotProps={{ htmlInput: { min: 1, max: 60 } }} sx={{ flex: 1 }} />
          </Stack>

          <Typography variant="body2" color={subject && schedule.length !== requiredBlocks ? "error" : "text.secondary"}>
            {subject
              ? `Bloques seleccionados: ${schedule.length} de ${requiredBlocks} (TEL ${requiredBlocks * 2} horas). Haz clic en la grilla para elegirlos.`
              : "Elige una asignatura para saber cuántos bloques necesita."}
          </Typography>
          <ScheduleGrid highlighted={schedule} onToggle={toggleBlock} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSave}
                disabled={!form.subjectId || !form.teacherId || schedule.length === 0}>
          Guardar
        </Button>
      </DialogActions>
    </Dialog>
  );
}