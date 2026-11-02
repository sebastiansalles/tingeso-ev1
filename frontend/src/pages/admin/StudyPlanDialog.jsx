import { useState } from "react";
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField,
} from "@mui/material";
import studyPlanService from "../../services/studyPlan.service";
import errorMessage from "../../utils/errorMessage";

// Creates a study plan for the selected career
export default function StudyPlanDialog({ careerId, onClose, onSaved }) {
  const [plan, setPlan] = useState({ identifier: "", status: "CURRENT" });
  const [error, setError] = useState(null);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setPlan((previous) => ({ ...previous, [name]: value }));
  };

  const handleSave = () => {
    studyPlanService.create({ ...plan, careerId })
      .then((response) => onSaved(response.data))
      .catch((err) => setError(errorMessage(err)));
  };

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Nuevo plan de estudios</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField label="Identificador" name="identifier" value={plan.identifier} onChange={handleChange}
                     placeholder="2027.1" required />
          <TextField select label="Estado" name="status" value={plan.status} onChange={handleChange}
                     helperText={plan.status === "CURRENT" ? "El plan vigente actual pasará a no vigente" : ""}>
            <MenuItem value="CURRENT">Vigente</MenuItem>
            <MenuItem value="NOT_CURRENT">No vigente</MenuItem>
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSave} disabled={!plan.identifier}>Guardar</Button>
      </DialogActions>
    </Dialog>
  );
}