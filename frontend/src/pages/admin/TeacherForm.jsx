import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Alert, Box, Button, MenuItem, Paper, Stack, TextField, Typography } from "@mui/material";
import teacherService from "../../services/teacher.service";
import errorMessage from "../../utils/errorMessage";
import { TEACHER_STATUS } from "../../constants";

const EMPTY = { run: "", fullName: "", email: "", professionalTitle: "", academicDegree: "", status: "ACTIVE" };

export default function TeacherForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [teacher, setTeacher] = useState(EMPTY);
  const [originalStatus, setOriginalStatus] = useState("ACTIVE");
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isEdit) return;
    teacherService.get(id)
      .then((response) => {
        setTeacher(response.data);
        setOriginalStatus(response.data.status);
      })
      .catch((err) => setError(errorMessage(err)));
  }, [id, isEdit]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setTeacher((previous) => ({ ...previous, [name]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setSaving(true);
    setError(null);

    // New teachers start as ACTIVE (set by the backend); in edit mode, status is a separate endpoint
    const request = !isEdit
      ? teacherService.create(teacher)
      : teacherService.update(id, teacher).then(() =>
          teacher.status !== originalStatus ? teacherService.changeStatus(id, teacher.status) : null);

    request
      .then(() => navigate("/teachers"))
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setSaving(false));
  };

  return (
    <Paper sx={{ p: 3, maxWidth: 600 }}>
      <Typography variant="h5" gutterBottom>{isEdit ? "Editar docente" : "Nuevo docente"}</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box component="form" onSubmit={handleSubmit}>
        <Stack spacing={2}>
          <TextField label="RUN" name="run" value={teacher.run} onChange={handleChange} required placeholder="9.876.543-2" />
          <TextField label="Nombre completo" name="fullName" value={teacher.fullName} onChange={handleChange} required />
          <TextField label="Correo electrónico" name="email" type="email" value={teacher.email} onChange={handleChange} required
                     helperText="Debe coincidir con el correo de su usuario en Keycloak" />
          <TextField label="Título profesional" name="professionalTitle" value={teacher.professionalTitle} onChange={handleChange} required />
          <TextField label="Grado académico" name="academicDegree" value={teacher.academicDegree} onChange={handleChange} required />

          {isEdit && (
            <TextField select label="Estado" name="status" value={teacher.status} onChange={handleChange}
                       helperText="No puede pasar a inactivo si tiene secciones en un período abierto">
              {Object.entries(TEACHER_STATUS).map(([value, label]) => (
                <MenuItem key={value} value={value}>{label}</MenuItem>
              ))}
            </TextField>
          )}

          <Stack direction="row" spacing={2}>
            <Button type="submit" variant="contained" disabled={saving}>
              {saving ? "Guardando..." : "Guardar"}
            </Button>
            <Button onClick={() => navigate("/teachers")}>Cancelar</Button>
          </Stack>
        </Stack>
      </Box>
    </Paper>
  );
}