// Turns an Axios error into a message for the user.
// The backend (GlobalExceptionHandler) answers business errors with a plain-text explanation.
export default function errorMessage(error) {
  const status = error.response?.status;
  const body = error.response?.data;

  if (!error.response) return "No se pudo conectar con el servidor.";
  if (status === 401) return "Tu sesión expiró. Vuelve a iniciar sesión.";
  if (status === 403) return "No tienes permiso para realizar esta acción.";
  if (typeof body === "string" && body && status < 500) return body;
  return "Ocurrió un error inesperado. Intenta nuevamente.";
}