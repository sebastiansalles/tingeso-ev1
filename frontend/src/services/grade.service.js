import api from "../http-common";

// Grades of a section, registered by its teacher
const getSectionEnrollments = (sectionId) => api.get(`/sections/${sectionId}/enrollments`);
const getSectionStudents = (sectionId) => api.get(`/sections/${sectionId}/students`);
const registerGrade = (enrollmentId, grade) => api.put(`/enrollments/${enrollmentId}/grade`, null, { params: { grade } });

export default { getSectionEnrollments, getSectionStudents, registerGrade };