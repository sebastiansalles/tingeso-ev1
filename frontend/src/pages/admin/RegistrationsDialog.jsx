import { useEffect, useState } from "react";
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, List, ListItem, ListItemText,
  MenuItem, Stack, TextField, Typography,
} from "@mui/material";
import periodService from "../../services/period.service";
import studentService from "../../services/student.service";
import errorMessage from "../../utils/errorMessage";

// Students registered ("matriculados") in a period. Only registered students can enroll subjects.
export default function RegistrationsDialog({ period, onClose }) {
  const [registrations, setRegistrations] = useState([]);
  const [students, setStudents] = useState([]);
  const [selected, setSelected] = useState("");
  const [version, setVersion] = useState(0);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([periodService.getRegistrations(period.id), studentService.getAll()])
      .then(([registrationsResponse, studentsResponse]) => {
        setRegistrations(registrationsResponse.data);
        setStudents(studentsResponse.data);
      })
      .catch((err) => setError(errorMessage(err)));
  }, [period.id, version]);

  const registeredIds = registrations.map((r) => r.studentId);
  const registered = students.filter((s) => registeredIds.includes(s.id));
  // Graduated or eliminated students cannot continue: they are not offered
  const candidates = students.filter(
    (s) => !registeredIds.includes(s.id) && s.status !== "GRADUATED" && s.status !== "ELIMINATED");

  const isOpen = period.status === "OPEN";

  const handleRegister = () => {
    setError(null);
    periodService.register(period.id, selected)
      .then(() => { setSelected(""); setVersion((v) => v + 1); })
      .catch((err) => setError(errorMessage(err)));
  };

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Matrícula {period.year}-{period.semester}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {isOpen && (
            <Stack direction="row" spacing={2}>
              <TextField select label="Matricular estudiante" value={selected} sx={{ flex: 1 }}
                         onChange={(event) => setSelected(event.target.value)} disabled={candidates.length === 0}>
                {candidates.map((s) => <MenuItem key={s.id} value={s.id}>{s.run} - {s.fullName}</MenuItem>)}
              </TextField>
              <Button variant="contained" onClick={handleRegister} disabled={!selected}>Matricular</Button>
            </Stack>
          )}
          <Typography variant="subtitle2">Matriculados ({registered.length})</Typography>
          {registered.length === 0
            ? <Typography color="text.secondary">Aún no hay estudiantes matriculados.</Typography>
            : (
              <List dense>
                {registered.map((s) => (
                  <ListItem key={s.id}><ListItemText primary={s.fullName} secondary={`${s.run} · ${s.email}`} /></ListItem>
                ))}
              </List>
            )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}