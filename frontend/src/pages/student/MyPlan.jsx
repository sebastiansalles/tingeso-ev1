import { useEffect, useState } from "react";
import {
  Alert, Chip, CircularProgress, Paper, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import studentService from "../../services/student.service";
import careerService from "../../services/career.service";
import studyPlanService from "../../services/studyPlan.service";
import subjectService from "../../services/subject.service";
import enrollmentService from "../../services/enrollment.service";
import errorMessage from "../../utils/errorMessage";
import { STUDENT_STATUS } from "../../constants";

// The logged-in student: personal data and their study plan, marking what is already approved
export default function MyPlan() {
  const [student, setStudent] = useState(null);
  const [career, setCareer] = useState(null);
  const [plan, setPlan] = useState(null);
  const [subjects, setSubjects] = useState([]);
  const [prerequisites, setPrerequisites] = useState({});   // subjectId -> [prerequisiteId]
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    studentService.getMe()
      .then((me) => {
        setStudent(me.data);
        return Promise.all([
          careerService.get(me.data.careerId),
          studyPlanService.get(me.data.studyPlanId),
          subjectService.getByStudyPlan(me.data.studyPlanId),
          enrollmentService.getAcademicHistory(me.data.id),
        ]);
      })
      .then(([careerResponse, planResponse, subjectsResponse, historyResponse]) => {
        const sorted = [...subjectsResponse.data].sort((a, b) => a.semester - b.semester || a.code.localeCompare(b.code));
        setCareer(careerResponse.data);
        setPlan(planResponse.data);
        setHistory(historyResponse.data);
        return Promise.all(sorted.map((s) => subjectService.getPrerequisites(s.id))).then((answers) => {
          const map = {};
          sorted.forEach((s, i) => { map[s.id] = answers[i].data.map((p) => p.prerequisiteId); });
          setSubjects(sorted);
          setPrerequisites(map);
        });
      })
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;

  // Situation of each subject according to the academic history (closed periods only)
  const approvedIds = history.filter((e) => e.result === "APPROVED").map((e) => e.subjectId);
  const failedIds = history.filter((e) => e.result === "FAILED").map((e) => e.subjectId);
  const codeOf = (id) => subjects.find((s) => s.id === id)?.code ?? id;
  const approvedSct = subjects.filter((s) => approvedIds.includes(s.id)).reduce((sum, s) => sum + s.sct, 0);
  const totalSct = subjects.reduce((sum, s) => sum + s.sct, 0);

  const situation = (subject) => {
    if (approvedIds.includes(subject.id)) return <Chip size="small" color="success" label="Aprobada" />;
    if (failedIds.includes(subject.id)) return <Chip size="small" color="warning" label="Reprobada" />;
    return <Chip size="small" label="Pendiente" />;
  };

  return (
    <>
      <Typography variant="h5" gutterBottom>Mi plan de estudios</Typography>
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack spacing={0.5}>
          <Typography><strong>{student.fullName}</strong> · {student.run} · {student.email}</Typography>
          <Typography>{career?.code} - {career?.name} · Plan {plan?.identifier}</Typography>
          <Typography>
            Estado: <strong>{STUDENT_STATUS[student.status]}</strong> · Avance: {approvedSct} de {totalSct} SCT aprobados
          </Typography>
        </Stack>
      </Paper>

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
              <TableCell>Situación</TableCell>
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
                <TableCell>{situation(s)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}