import { useState } from "react";
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField,
} from "@mui/material";
import periodService from "../../services/period.service";
import errorMessage from "../../utils/errorMessage";

// Creates an academic period, e.g. 2027-1. It always starts OPEN (set by the backend).
export default function PeriodDialog({ onClose, onSaved }) {
  const [period, setPeriod] = useState({ year: new Date().getFullYear() + 1, semester: 1 });
  const [error, setError] = useState(null);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setPeriod((previous) => ({ ...previous, [name]: Number(value) }));
  };

  const handleSave = () => {
    periodService.create(period)
      .then(() => onSaved())
      .catch((err) => setError(errorMessage(err)));
  };

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Nuevo período académico</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField type="number" label="Año" name="year" value={period.year} onChange={handleChange} />
          <TextField select label="Semestre" name="semester" value={period.semester} onChange={handleChange}
                     helperText={`Se creará el período ${period.year}-${period.semester}, en estado abierto`}>
            <MenuItem value={1}>1</MenuItem>
            <MenuItem value={2}>2</MenuItem>
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSave}>Guardar</Button>
      </DialogActions>
    </Dialog>
  );
}