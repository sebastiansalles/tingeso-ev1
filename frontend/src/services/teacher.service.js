import api from "../http-common";

// /api/v1/teachers
const getAll = () => api.get("/teachers");
const get = (id) => api.get(`/teachers/${id}`);
const create = (teacher) => api.post("/teachers", teacher);
const update = (id, teacher) => api.put(`/teachers/${id}`, teacher);
const changeStatus = (id, status) => api.put(`/teachers/${id}/status`, null, { params: { status } });

export default { getAll, get, create, update, changeStatus };