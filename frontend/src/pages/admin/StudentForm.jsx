import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Alert, Box, Button, MenuItem, Paper, Stack, TextField, Typography } from "@mui/material";
import studentService from "../../services/student.service";
import careerService from "../../services/career.service";
import errorMessage from "../../utils/errorMessage";
import { MANUAL_STUDENT_STATUSES, STUDENT_STATUS } from "../../constants";

const EMPTY = { run: "", fullName: "", email: "", careerId: "", status: "REGULAR" };

export default function StudentForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [student, setStudent] = useState(EMPTY);
  const [originalStatus, setOriginalStatus] = useState("REGULAR");
  const [careers, setCareers] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  // Careers for the select; in edit mode, also the student's current data
  useEffect(() => {
    careerService.getAll()
      .then((response) => setCareers(response.data))
      .catch((err) => setError(errorMessage(err)));
    if (!isEdit) return;
    studentService.get(id)
      .then((response) => {
        setStudent(response.data);
        setOriginalStatus(response.data.status);
      })
      .catch((err) => setError(errorMessage(err)));
  }, [id, isEdit]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setStudent((previous) => ({ ...previous, [name]: value }));
  };

  // GRADUATED and ELIMINATED are final: assigned only by the period closing
  const statusLocked = !MANUAL_STUDENT_STATUSES.includes(originalStatus);

  const handleSubmit = (event) => {
    event.preventDefault();
    setSaving(true);
    setError(null);

    let request;
    if (!isEdit) {
      // Plan and initial status (REGULAR) are assigned by the backend
      request = studentService.create(student);
    } else {
      // Personal data and status are separate endpoints in the backend
      request = studentService.update(id, student).then(() =>
        student.status !== originalStatus ? studentService.changeStatus(id, student.status) : null);
    }

    request
      .then(() => navigate("/students"))
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setSaving(false));
  };

  // A new student can only join an active career
  const careerOptions = isEdit ? careers : careers.filter((c) => c.status === "ACTIVE");

  return (
    <Paper sx={{ p: 3, maxWidth: 600 }}>
      <Typography variant="h5" gutterBottom>{isEdit ? "Editar estudiante" : "Nuevo estudiante"}</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box component="form" onSubmit={handleSubmit}>
        <Stack spacing={2}>
          <TextField label="RUN" name="run" value={student.run} onChange={handleChange} required
                     placeholder="12.345.678-9" />
          <TextField label="Nombre completo" name="fullName" value={student.fullName} onChange={handleChange} required />
          <TextField label="Correo electrónico" name="email" type="email" value={student.email} onChange={handleChange} required
                     disabled={isEdit}
                     helperText={isEdit ? "Vincula al estudiante con su cuenta de acceso; no se puede modificar" : "Será su usuario de acceso. La contraseña inicial será su RUN"} />
          <TextField select label="Carrera" name="careerId" value={student.careerId} onChange={handleChange} required
                     disabled={isEdit}
                     helperText={isEdit ? "La carrera no se modifica desde aquí" : "Se asignará el plan de estudios vigente"}>
            {careerOptions.map((c) => <MenuItem key={c.id} value={c.id}>{c.code} - {c.name}</MenuItem>)}
          </TextField>

          {isEdit && (
            <TextField select label="Estado" name="status" value={student.status} onChange={handleChange}
                       disabled={statusLocked}
                       helperText={statusLocked ? "Estado asignado en el cierre de período; no se puede cambiar" : ""}>
              {(statusLocked ? [originalStatus] : MANUAL_STUDENT_STATUSES).map((s) => (
                <MenuItem key={s} value={s}>{STUDENT_STATUS[s]}</MenuItem>
              ))}
            </TextField>
          )}

          <Stack direction="row" spacing={2}>
            <Button type="submit" variant="contained" disabled={saving}>
              {saving ? "Guardando..." : "Guardar"}
            </Button>
            <Button onClick={() => navigate("/students")}>Cancelar</Button>
          </Stack>
        </Stack>
      </Box>
    </Paper>
  );
}