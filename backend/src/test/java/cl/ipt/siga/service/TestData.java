package cl.ipt.siga.service;

import cl.ipt.siga.entity.*;
import cl.ipt.siga.entity.enums.*;

// Builders for the entities used by the service unit tests.
// Keeps the tests short: each test only sets what matters for its scenario.
final class TestData {

    private TestData() { }

    static CareerEntity career(Long id, String code, CareerStatus status) {
        CareerEntity career = new CareerEntity();
        career.setId(id);
        career.setCode(code);
        career.setName("Técnico Analista Programador");
        career.setDescription("Two-year technical career");
        career.setStatus(status);
        return career;
    }

    static StudyPlanEntity studyPlan(Long id, Long careerId, String identifier, StudyPlanStatus status) {
        StudyPlanEntity plan = new StudyPlanEntity();
        plan.setId(id);
        plan.setCareerId(careerId);
        plan.setIdentifier(identifier);
        plan.setStatus(status);
        return plan;
    }

    // Subject with TEL 2-0-2 (4 hours = 2 schedule blocks) and 5 SCT
    static SubjectEntity subject(Long id, Long studyPlanId, String code, int semester) {
        return subject(id, studyPlanId, code, semester, 2, 0, 2, 5);
    }

    static SubjectEntity subject(Long id, Long studyPlanId, String code, int semester,
                                 int theory, int exercise, int lab, int sct) {
        SubjectEntity subject = new SubjectEntity();
        subject.setId(id);
        subject.setStudyPlanId(studyPlanId);
        subject.setCode(code);
        subject.setName("Subject " + code);
        subject.setSemester(semester);
        subject.setTheoryHours(theory);
        subject.setExerciseHours(exercise);
        subject.setLabHours(lab);
        subject.setSct(sct);
        return subject;
    }

    static PrerequisiteEntity prerequisite(Long subjectId, Long prerequisiteId) {
        PrerequisiteEntity prerequisite = new PrerequisiteEntity();
        prerequisite.setSubjectId(subjectId);
        prerequisite.setPrerequisiteId(prerequisiteId);
        return prerequisite;
    }

    static StudentEntity student(Long id, String run, String email, StudentStatus status) {
        StudentEntity student = new StudentEntity();
        student.setId(id);
        student.setRun(run);
        student.setFullName("Ana Pérez");
        student.setEmail(email);
        student.setCareerId(1L);
        student.setStudyPlanId(10L);
        student.setStatus(status);
        return student;
    }

    static TeacherEntity teacher(Long id, String run, String email, TeacherStatus status) {
        TeacherEntity teacher = new TeacherEntity();
        teacher.setId(id);
        teacher.setRun(run);
        teacher.setFullName("Carlos Rojas");
        teacher.setEmail(email);
        teacher.setProfessionalTitle("Ingeniero Civil Informático");
        teacher.setAcademicDegree("Magíster");
        teacher.setStatus(status);
        return teacher;
    }

    static AcademicPeriodEntity period(Long id, int year, int semester, PeriodStatus status) {
        AcademicPeriodEntity period = new AcademicPeriodEntity();
        period.setId(id);
        period.setYear(year);
        period.setSemester(semester);
        period.setStatus(status);
        return period;
    }

    static SectionEntity section(Long id, Long subjectId, Long academicPeriodId, Long teacherId, int capacity) {
        SectionEntity section = new SectionEntity();
        section.setId(id);
        section.setSubjectId(subjectId);
        section.setAcademicPeriodId(academicPeriodId);
        section.setTeacherId(teacherId);
        section.setCapacity(capacity);
        return section;
    }

    static ScheduleBlockEntity block(Long sectionId, Day day, int module) {
        ScheduleBlockEntity block = new ScheduleBlockEntity();
        block.setSectionId(sectionId);
        block.setDay(day);
        block.setModule(module);
        return block;
    }

    static EnrollmentEntity enrollment(Long id, Long studentId, Long subjectId, Long sectionId,
                                       Long academicPeriodId, Double finalGrade, SubjectResult result) {
        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setId(id);
        enrollment.setStudentId(studentId);
        enrollment.setSubjectId(subjectId);
        enrollment.setSectionId(sectionId);
        enrollment.setAcademicPeriodId(academicPeriodId);
        enrollment.setSubjectCode("SUB" + subjectId);
        enrollment.setSubjectName("Subject " + subjectId);
        enrollment.setSubjectSct(5);
        enrollment.setFinalGrade(finalGrade);
        enrollment.setResult(result);
        return enrollment;
    }
}
