import api from "../http-common";

// /api/v1/subjects and /api/v1/subjects/{id}/prerequisites
const getByStudyPlan = (studyPlanId) => api.get("/subjects", { params: { studyPlanId } });
const create = (subject) => api.post("/subjects", subject);
const update = (id, subject) => api.put(`/subjects/${id}`, subject);

const getPrerequisites = (subjectId) => api.get(`/subjects/${subjectId}/prerequisites`);
const addPrerequisite = (subjectId, prerequisiteId) => api.post(`/subjects/${subjectId}/prerequisites/${prerequisiteId}`);
const removePrerequisite = (subjectId, prerequisiteId) => api.delete(`/subjects/${subjectId}/prerequisites/${prerequisiteId}`);
const getAll = () => api.get("/subjects");

export default { getAll, getByStudyPlan, create, update, getPrerequisites, addPrerequisite, removePrerequisite };