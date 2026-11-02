import { useEffect, useState } from "react";
import {
  Alert, Button, IconButton, MenuItem, Paper, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, Tooltip, Typography,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import AccountTreeIcon from "@mui/icons-material/AccountTree";
import careerService from "../../services/career.service";
import studyPlanService from "../../services/studyPlan.service";
import subjectService from "../../services/subject.service";
import errorMessage from "../../utils/errorMessage";
import StudyPlanDialog from "./StudyPlanDialog";
import SubjectDialog from "./SubjectDialog";
import PrerequisitesDialog from "./PrerequisitesDialog";

// Study plans of a career and the subjects (with prerequisites) of the selected plan
export default function SubjectList() {
  const [careers, setCareers] = useState([]);
  const [careerId, setCareerId] = useState("");
  const [plans, setPlans] = useState([]);
  const [planId, setPlanId] = useState("");
  const [subjects, setSubjects] = useState([]);
  const [prerequisites, setPrerequisites] = useState({});   // subjectId -> [prerequisiteId, ...]
  const [error, setError] = useState(null);
  const [plansVersion, setPlansVersion] = useState(0);      // increase it to reload the plans
  const [subjectsVersion, setSubjectsVersion] = useState(0); // increase it to reload the subjects
  const [dialog, setDialog] = useState(null);               // which dialog is open, if any

  // 1. On entering: load the careers and select the first one
  useEffect(() => {
    careerService.getAll()
      .then((response) => {
        setCareers(response.data);
        if (response.data.length > 0) setCareerId(response.data[0].id);
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  // 2. When the career changes: load its plans and select the current one
  useEffect(() => {
    if (!careerId) return;
    studyPlanService.getByCareer(careerId)
      .then((response) => {
        setPlans(response.data);
        const current = response.data.find((p) => p.status === "CURRENT") ?? response.data[0];
        setPlanId(current ? current.id : "");
        if (!current) { setSubjects([]); setPrerequisites({}); }
      })
      .catch((err) => setError(errorMessage(err)));
  }, [careerId, plansVersion]);

  // 3. When the plan changes: load its subjects and, in parallel, the prerequisites of each one
  useEffect(() => {
    if (!planId) return;
    subjectService.getByStudyPlan(planId)
      .then((response) => {
        const sorted = [...response.data].sort((a, b) => a.semester - b.semester || a.code.localeCompare(b.code));
        return Promise.all(sorted.map((s) => subjectService.getPrerequisites(s.id)))
          .then((answers) => {
            const map = {};
            sorted.forEach((s, i) => { map[s.id] = answers[i].data.map((p) => p.prerequisiteId); });
            setSubjects(sorted);
            setPrerequisites(map);
          });
      })
      .catch((err) => setError(errorMessage(err)));
  }, [planId, subjectsVersion]);

  const codeOf = (id) => subjects.find((s) => s.id === id)?.code ?? id;
  const closeDialog = () => setDialog(null);
  const reloadSubjects = () => setSubjectsVersion((v) => v + 1);

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Planes de estudio y asignaturas</Typography>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" disabled={!careerId} onClick={() => setDialog({ type: "plan" })}>Nuevo plan</Button>
          <Button variant="contained" disabled={!planId} onClick={() => setDialog({ type: "subject" })}>Nueva asignatura</Button>
        </Stack>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField select label="Carrera" value={careerId} onChange={(e) => setCareerId(e.target.value)} sx={{ minWidth: 320 }}>
          {careers.map((c) => <MenuItem key={c.id} value={c.id}>{c.code} - {c.name}</MenuItem>)}
        </TextField>
        <TextField select label="Plan de estudios" value={planId} onChange={(e) => setPlanId(e.target.value)} sx={{ minWidth: 200 }}
                   disabled={plans.length === 0}>
          {plans.map((p) => (
            <MenuItem key={p.id} value={p.id}>{p.identifier}{p.status === "CURRENT" ? " (vigente)" : ""}</MenuItem>
          ))}
        </TextField>
      </Stack>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Sem.</TableCell>
              <TableCell>Código</TableCell>
              <TableCell>Asignatura</TableCell>
              <TableCell>T-E-L</TableCell>
              <TableCell>SCT</TableCell>
              <TableCell>Prerrequisitos</TableCell>
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {subjects.map((s) => (
              <TableRow key={s.id}>
                <TableCell>{s.semester}</TableCell>
                <TableCell>{s.code}</TableCell>
                <TableCell>{s.name}</TableCell>
                <TableCell>{s.theoryHours}-{s.exerciseHours}-{s.labHours}</TableCell>
                <TableCell>{s.sct}</TableCell>
                <TableCell>{(prerequisites[s.id] ?? []).map(codeOf).join(", ") || "—"}</TableCell>
                <TableCell align="right">
                  <Tooltip title="Editar">
                    <IconButton size="small" onClick={() => setDialog({ type: "subject", subject: s })}><EditIcon /></IconButton>
                  </Tooltip>
                  <Tooltip title="Prerrequisitos">
                    <IconButton size="small" onClick={() => setDialog({ type: "prerequisites", subject: s })}><AccountTreeIcon /></IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
            {subjects.length === 0 && (
              <TableRow><TableCell colSpan={7}>Este plan no tiene asignaturas.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Dialogs are rendered only while open, so each one starts with a fresh, empty state */}
      {dialog?.type === "plan" && (
        <StudyPlanDialog careerId={careerId} onClose={closeDialog}
                         onSaved={() => { closeDialog(); setPlansVersion((v) => v + 1); }} />
      )}
      {dialog?.type === "subject" && (
        <SubjectDialog studyPlanId={planId} subject={dialog.subject} onClose={closeDialog}
                       onSaved={() => { closeDialog(); reloadSubjects(); }} />
      )}
      {dialog?.type === "prerequisites" && (
        <PrerequisitesDialog subject={dialog.subject} planSubjects={subjects}
                             prerequisiteIds={prerequisites[dialog.subject.id] ?? []}
                             onClose={closeDialog} onChanged={reloadSubjects} />
      )}
    </>
  );
}