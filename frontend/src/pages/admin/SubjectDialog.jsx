import { useState } from "react";
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography,
} from "@mui/material";
import subjectService from "../../services/subject.service";
import errorMessage from "../../utils/errorMessage";

const EMPTY = { code: "", name: "", semester: 1, theoryHours: 2, exerciseHours: 0, labHours: 2, sct: 5 };
const NUMBER_FIELDS = ["semester", "theoryHours", "exerciseHours", "labHours", "sct"];

// Creates a subject in the plan, or edits it when "subject" is given
export default function SubjectDialog({ studyPlanId, subject, onClose, onSaved }) {
  const isEdit = Boolean(subject);
  const [form, setForm] = useState(subject ?? EMPTY);
  const [error, setError] = useState(null);

  // Derived values: calculated from the form on every render, never stored (same rule as in the backend)
  const tel = Number(form.theoryHours) + Number(form.exerciseHours) + Number(form.labHours);
  const blocks = tel / 2;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  };

  const handleSave = () => {
    // Inputs always give text: numeric fields are converted before sending them
    const payload = { ...form, studyPlanId };
    NUMBER_FIELDS.forEach((field) => { payload[field] = Number(form[field]); });

    const request = isEdit ? subjectService.update(subject.id, payload) : subjectService.create(payload);
    request
      .then(() => onSaved())
      .catch((err) => setError(errorMessage(err)));
  };

  const numberField = (name, label) => (
    <TextField type="number" label={label} name={name} value={form[name]} onChange={handleChange}
               slotProps={{ htmlInput: { min: 0, max: 7 } }} sx={{ flex: 1 }} />
  );

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{isEdit ? `Editar ${subject.code}` : "Nueva asignatura"}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Stack direction="row" spacing={2}>
            <TextField label="Código" name="code" value={form.code} onChange={handleChange} required sx={{ flex: 1 }} />
            <TextField select label="Semestre" name="semester" value={form.semester} onChange={handleChange} sx={{ flex: 1 }}>
              {[1, 2, 3, 4].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          </Stack>
          <TextField label="Nombre" name="name" value={form.name} onChange={handleChange} required />
          <Stack direction="row" spacing={2}>
            {numberField("theoryHours", "Teoría")}
            {numberField("exerciseHours", "Ejercicios")}
            {numberField("labHours", "Laboratorio")}
            {numberField("sct", "SCT")}
          </Stack>
          <Typography variant="body2" color={tel >= 8 ? "error" : "text.secondary"}>
            TEL: {tel} horas semanales ({Number.isInteger(blocks) ? `${blocks} bloques` : "debe ser par"}). Máximo 7.
          </Typography>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSave} disabled={!form.code || !form.name}>Guardar</Button>
      </DialogActions>
    </Dialog>
  );
}