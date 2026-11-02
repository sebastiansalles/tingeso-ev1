import api from "../http-common";

// /api/v1/sections
const getByPeriod = (academicPeriodId) => api.get("/sections", { params: { academicPeriodId } });
const getSchedule = (id) => api.get(`/sections/${id}/schedule`);
// request: { subjectId, academicPeriodId, teacherId, capacity, schedule: ["L1", "W1"] }
const create = (request) => api.post("/sections", request);

const get = (id) => api.get(`/sections/${id}`);

export default { getByPeriod, getSchedule, create, get };