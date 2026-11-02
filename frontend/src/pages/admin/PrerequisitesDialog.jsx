import { useState } from "react";
import {
  Alert, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography,
} from "@mui/material";
import subjectService from "../../services/subject.service";
import errorMessage from "../../utils/errorMessage";

// Shows and edits the prerequisites of one subject.
// "planSubjects" are all the subjects of the plan, used to show codes and to offer candidates.
export default function PrerequisitesDialog({ subject, prerequisiteIds, planSubjects, onClose, onChanged }) {
  const [selected, setSelected] = useState("");
  const [error, setError] = useState(null);

  const byId = (id) => planSubjects.find((s) => s.id === id);

  // Only subjects of earlier semesters that are not already prerequisites (the backend validates it again)
  const candidates = planSubjects.filter(
    (s) => s.semester < subject.semester && !prerequisiteIds.includes(s.id));

  const run = (request) => {
    setError(null);
    request.then(() => { setSelected(""); onChanged(); })
      .catch((err) => setError(errorMessage(err)));
  };

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Prerrequisitos de {subject.code}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {prerequisiteIds.length === 0
            ? <Typography color="text.secondary">Sin prerrequisitos.</Typography>
            : (
              <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
                {prerequisiteIds.map((id) => (
                  <Chip key={id} label={byId(id)?.code ?? id}
                        onDelete={() => run(subjectService.removePrerequisite(subject.id, id))} />
                ))}
              </Stack>
            )}
          <Stack direction="row" spacing={2}>
            <TextField select label="Agregar prerrequisito" value={selected} sx={{ flex: 1 }}
                       onChange={(event) => setSelected(event.target.value)}
                       disabled={candidates.length === 0 || prerequisiteIds.length >= 3}
                       helperText={prerequisiteIds.length >= 3 ? "Máximo 3 prerrequisitos" : "Solo asignaturas de semestres anteriores"}>
              {candidates.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} - {s.name}</MenuItem>)}
            </TextField>
            <Button variant="contained" disabled={!selected}
                    onClick={() => run(subjectService.addPrerequisite(subject.id, selected))}>
              Agregar
            </Button>
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}