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