import api from "../http-common";

// Enrollment of the logged-in student: /api/v1/students/{studentId}/... and /api/v1/enrollments/{id}
const getAvailableSubjects = (studentId, academicPeriodId) =>
  api.get(`/students/${studentId}/available-subjects`, { params: { academicPeriodId } });
// subjectIds: [1, 2, 3] -> enrolls all of them, or none if any rule fails
const enroll = (studentId, academicPeriodId, subjectIds) =>
  api.post(`/students/${studentId}/enrollments`, subjectIds, { params: { academicPeriodId } });
const getEnrollments = (studentId, academicPeriodId) =>
  api.get(`/students/${studentId}/enrollments`, { params: { academicPeriodId } });
const getAcademicHistory = (studentId) => api.get(`/students/${studentId}/academic-history`);
const cancel = (enrollmentId) => api.delete(`/enrollments/${enrollmentId}`);

export default { getAvailableSubjects, enroll, getEnrollments, getAcademicHistory, cancel };