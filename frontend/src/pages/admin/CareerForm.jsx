import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Alert, Box, Button, MenuItem, Paper, Stack, TextField, Typography } from "@mui/material";
import careerService from "../../services/career.service";
import errorMessage from "../../utils/errorMessage";

const EMPTY = { code: "", name: "", description: "", status: "ACTIVE" };

export default function CareerForm() {
  const { id } = useParams();          // present in /careers/:id/edit, absent in /careers/new
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [career, setCareer] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  // Edit mode: load the current data into the form
  useEffect(() => {
    if (!isEdit) return;
    careerService.get(id)
      .then((response) => setCareer(response.data))
      .catch((err) => setError(errorMessage(err)));
  }, [id, isEdit]);

  // One handler for every field: uses the input's "name" to know which property to change
  const handleChange = (event) => {
    const { name, value } = event.target;
    setCareer((previous) => ({ ...previous, [name]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();            // a form would otherwise reload the whole page
    setSaving(true);
    setError(null);
    const request = isEdit ? careerService.update(id, career) : careerService.create(career);
    request
      .then(() => navigate("/careers"))
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setSaving(false));
  };

  return (
    <Paper sx={{ p: 3, maxWidth: 600 }}>
      <Typography variant="h5" gutterBottom>{isEdit ? "Editar carrera" : "Nueva carrera"}</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box component="form" onSubmit={handleSubmit}>
        <Stack spacing={2}>
          <TextField label="Código" name="code" value={career.code} onChange={handleChange}
                     required disabled={isEdit}
                     helperText={isEdit ? "El código de una carrera no se puede modificar" : ""} />
          <TextField label="Nombre" name="name" value={career.name} onChange={handleChange} required />
          <TextField label="Descripción" name="description" value={career.description} onChange={handleChange}
                     required multiline rows={3} />
          <TextField select label="Estado" name="status" value={career.status} onChange={handleChange}>
            <MenuItem value="ACTIVE">Activa</MenuItem>
            <MenuItem value="INACTIVE">Inactiva</MenuItem>
          </TextField>

          <Stack direction="row" spacing={2}>
            <Button type="submit" variant="contained" disabled={saving}>
              {saving ? "Guardando..." : "Guardar"}
            </Button>
            <Button onClick={() => navigate("/careers")}>Cancelar</Button>
          </Stack>
        </Stack>
      </Box>
    </Paper>
  );
}