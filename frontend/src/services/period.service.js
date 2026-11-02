import api from "../http-common";

// /api/v1/academic-periods and its registrations ("matrícula")
const getAll = () => api.get("/academic-periods");
const create = (period) => api.post("/academic-periods", period);
const close = (id) => api.post(`/academic-periods/${id}/close`);

const getRegistrations = (periodId) => api.get(`/academic-periods/${periodId}/registrations`);
const register = (periodId, studentId) => api.post(`/academic-periods/${periodId}/registrations/${studentId}`);

export default { getAll, create, close, getRegistrations, register };