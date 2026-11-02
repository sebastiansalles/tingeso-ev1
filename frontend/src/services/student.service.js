import api from "../http-common";

// /api/v1/students
const getAll = () => api.get("/students");
const get = (id) => api.get(`/students/${id}`);
const create = (student) => api.post("/students", student);
const update = (id, student) => api.put(`/students/${id}`, student);
const changeStatus = (id, status) => api.put(`/students/${id}/status`, null, { params: { status } });

export default { getAll, get, create, update, changeStatus };