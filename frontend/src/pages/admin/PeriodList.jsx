import { useEffect, useState } from "react";
import {
  Alert, Button, Chip, Paper, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import periodService from "../../services/period.service";
import errorMessage from "../../utils/errorMessage";
import PeriodDialog from "./PeriodDialog";
import RegistrationsDialog from "./RegistrationsDialog";

// Academic periods: create, registrations ("matrícula") and closing
export default function PeriodList() {
  const [periods, setPeriods] = useState([]);
  const [version, setVersion] = useState(0);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [dialog, setDialog] = useState(null);

  useEffect(() => {
    periodService.getAll()
      .then((response) => {
        // Most recent first: 2027-2, 2027-1, 2026-2...
        const sorted = [...response.data].sort((a, b) => b.year - a.year || b.semester - a.semester);
        setPeriods(sorted);
      })
      .catch((err) => setError(errorMessage(err)));
  }, [version]);

  const reload = () => setVersion((v) => v + 1);

  const handleClose = (period) => {
    const name = `${period.year}-${period.semester}`;
    if (!window.confirm(`¿Cerrar el período ${name}? Las calificaciones pasarán al historial y no podrán modificarse.`)) {
      return;
    }
    setError(null);
    setSuccess(null);
    periodService.close(period.id)
      .then(() => { setSuccess(`Período ${name} cerrado.`); reload(); })
      .catch((err) => setError(errorMessage(err)));
  };

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Períodos académicos</Typography>
        <Button variant="contained" onClick={() => setDialog({ type: "period" })}>Nuevo período</Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>{success}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Período</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {periods.map((period) => (
              <TableRow key={period.id}>
                <TableCell>{period.year}-{period.semester}</TableCell>
                <TableCell>
                  <Chip size="small" label={period.status === "OPEN" ? "Abierto" : "Cerrado"}
                        color={period.status === "OPEN" ? "success" : "default"} />
                </TableCell>
                <TableCell align="right">
                  <Stack direction="row" spacing={1} sx={{ justifyContent: "flex-end" }}>
                    <Button size="small" onClick={() => setDialog({ type: "registrations", period })}>Matrícula</Button>
                    {period.status === "OPEN" && (
                      <Button size="small" color="error" onClick={() => handleClose(period)}>Cerrar período</Button>
                    )}
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
            {periods.length === 0 && (
              <TableRow><TableCell colSpan={3}>No hay períodos académicos.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {dialog?.type === "period" && (
        <PeriodDialog onClose={() => setDialog(null)} onSaved={() => { setDialog(null); reload(); }} />
      )}
      {dialog?.type === "registrations" && (
        <RegistrationsDialog period={dialog.period} onClose={() => setDialog(null)} />
      )}
    </>
  );
}