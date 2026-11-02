import { Box, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from "@mui/material";
import { DAYS, MODULES } from "../constants";

// Weekly grid: one row per module (M1..M6), one column per day (L, M, W, J, V).
// "cells" maps a block code ("L2") to the text shown in that cell.
// If "onToggle" is given, cells are clickable (used to choose the blocks of a section).
export default function ScheduleGrid({ cells = {}, onToggle, highlighted = [] }) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small" sx={{ tableLayout: "fixed" }}>
        <TableHead>
          <TableRow>
            <TableCell sx={{ width: 110 }}>Módulo</TableCell>
            {DAYS.map((day) => <TableCell key={day.code} align="center">{day.label}</TableCell>)}
          </TableRow>
        </TableHead>
        <TableBody>
          {MODULES.map((module) => (
            <TableRow key={module.number}>
              <TableCell>
                <Box sx={{ fontWeight: "bold" }}>M{module.number}</Box>
                <Box sx={{ fontSize: 12, color: "text.secondary" }}>{module.time}</Box>
              </TableCell>
              {DAYS.map((day) => {
                const code = `${day.code}${module.number}`;
                const selected = highlighted.includes(code);
                return (
                  <TableCell key={code} align="center"
                             onClick={onToggle ? () => onToggle(code) : undefined}
                             sx={{
                               cursor: onToggle ? "pointer" : "default",
                               bgcolor: selected ? "primary.main" : cells[code] ? "action.selected" : "inherit",
                               color: selected ? "primary.contrastText" : "inherit",
                               border: 1, borderColor: "divider", fontSize: 12,
                             }}>
                    {cells[code] ?? (selected ? code : "")}
                  </TableCell>
                );
              })}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}