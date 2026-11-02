import api from "../http-common";

// /api/v1/study-plans
const getByCareer = (careerId) => api.get("/study-plans", { params: { careerId } });
const create = (plan) => api.post("/study-plans", plan);
const get = (id) => api.get(`/study-plans/${id}`);

export default { getByCareer, get, create };

