import api from "../http-common";

// One function per backend endpoint of /api/v1/careers
const getAll = () => api.get("/careers");
const get = (id) => api.get(`/careers/${id}`);
const create = (career) => api.post("/careers", career);
const update = (id, career) => api.put(`/careers/${id}`, career);

export default { getAll, get, create, update };