// Labels shown to the user for the status values that come from the backend
export const STUDENT_STATUS = {
  REGULAR: "Regular",
  POSTPONED: "Postergación",
  TEMPORARY_WITHDRAWAL: "Retiro temporal",
  GRADUATED: "Egresado",
  ELIMINATED: "Eliminado",
};

// Only these can be assigned by the administrator; the others are set at period closing
export const MANUAL_STUDENT_STATUSES = ["REGULAR", "POSTPONED", "TEMPORARY_WITHDRAWAL"];

export const TEACHER_STATUS = {
  ACTIVE: "Activo",
  INACTIVE: "Inactivo",
};



// Schedule of the institute: day codes used in "L2", "W3"... and module times
export const DAYS = [
  { code: "L", value: "MONDAY", label: "Lunes" },
  { code: "M", value: "TUESDAY", label: "Martes" },
  { code: "W", value: "WEDNESDAY", label: "Miércoles" },
  { code: "J", value: "THURSDAY", label: "Jueves" },
  { code: "V", value: "FRIDAY", label: "Viernes" },
];

export const MODULES = [
  { number: 1, time: "08:15 - 09:35" },
  { number: 2, time: "09:50 - 11:10" },
  { number: 3, time: "11:25 - 12:45" },
  { number: 4, time: "13:45 - 15:05" },
  { number: 5, time: "15:20 - 16:40" },
  { number: 6, time: "16:55 - 18:15" },
];

// A schedule block from the backend ({ day: "MONDAY", module: 2 }) as the institute writes it: "L2"
export const blockCode = (block) => `${DAYS.find((d) => d.value === block.day)?.code}${block.module}`;