package cl.ipt.siga.integration;

import cl.ipt.siga.dto.SectionRequest;
import cl.ipt.siga.entity.*;
import cl.ipt.siga.entity.enums.CareerStatus;
import cl.ipt.siga.entity.enums.StudyPlanStatus;
import cl.ipt.siga.repository.EnrollmentRepository;
import cl.ipt.siga.repository.ScheduleBlockRepository;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import cl.ipt.siga.security.KeycloakAdminClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// Integration tests: the real services, repositories and database (H2 in memory, "test" profile)
// work together. Only Keycloak, an external system, is replaced by a mock. Each test runs inside
// a transaction that is rolled back at the end, so every test starts from an empty database
// and tests never affect each other.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestBase {
    // Keycloak is an external system: in the integration tests it is replaced by a mock
    @MockitoBean protected KeycloakAdminClient keycloakAdminClient;

    @Autowired protected CareerService careerService;
    @Autowired protected StudyPlanService studyPlanService;
    @Autowired protected SubjectService subjectService;
    @Autowired protected PrerequisiteService prerequisiteService;
    @Autowired protected TeacherService teacherService;
    @Autowired protected StudentService studentService;
    @Autowired protected AcademicPeriodService academicPeriodService;
    @Autowired protected PeriodRegistrationService periodRegistrationService;
    @Autowired protected SectionService sectionService;
    @Autowired protected EnrollmentService enrollmentService;
    @Autowired protected GradeService gradeService;
    @Autowired protected PeriodClosingService periodClosingService;

    @Autowired protected SectionRepository sectionRepository;
    @Autowired protected ScheduleBlockRepository scheduleBlockRepository;
    @Autowired protected EnrollmentRepository enrollmentRepository;

    // ---------------------------------------------------------------- data created through the services

    protected CareerEntity career(String code) {
        CareerEntity career = new CareerEntity();
        career.setCode(code);
        career.setName("Career " + code);
        career.setDescription("Two-year technical career");
        career.setStatus(CareerStatus.ACTIVE);
        return careerService.create(career);
    }

    protected StudyPlanEntity currentPlan(CareerEntity career, String identifier) {
        StudyPlanEntity plan = new StudyPlanEntity();
        plan.setCareerId(career.getId());
        plan.setIdentifier(identifier);
        plan.setStatus(StudyPlanStatus.CURRENT);
        return studyPlanService.create(plan);
    }

    // TEL t-e-l: every 2 hours is one schedule block
    protected SubjectEntity subject(StudyPlanEntity plan, String code, int semester, int theory, int exercise, int lab) {
        SubjectEntity subject = new SubjectEntity();
        subject.setStudyPlanId(plan.getId());
        subject.setCode(code);
        subject.setName("Subject " + code);
        subject.setSemester(semester);
        subject.setTheoryHours(theory);
        subject.setExerciseHours(exercise);
        subject.setLabHours(lab);
        subject.setSct(5);
        return subjectService.create(subject);
    }

    protected TeacherEntity teacher(String run, String email) {
        TeacherEntity teacher = new TeacherEntity();
        teacher.setRun(run);
        teacher.setFullName("Teacher " + email);
        teacher.setEmail(email);
        teacher.setProfessionalTitle("Ingeniero Civil Informático");
        teacher.setAcademicDegree("Magíster");
        return teacherService.create(teacher);
    }

    protected StudentEntity student(CareerEntity career, String run, String email) {
        StudentEntity student = new StudentEntity();
        student.setRun(run);
        student.setFullName("Student " + email);
        student.setEmail(email);
        student.setCareerId(career.getId());
        return studentService.create(student);
    }

    protected AcademicPeriodEntity period(int year, int semester) {
        AcademicPeriodEntity period = new AcademicPeriodEntity();
        period.setYear(year);
        period.setSemester(semester);
        return academicPeriodService.create(period);
    }

    protected SectionEntity section(SubjectEntity subject, AcademicPeriodEntity period, TeacherEntity teacher,
                                    int capacity, String... schedule) {
        return sectionService.create(new SectionRequest(
                subject.getId(), period.getId(), teacher.getId(), capacity, Arrays.asList(schedule)));
    }

    protected void register(StudentEntity student, AcademicPeriodEntity period) {
        periodRegistrationService.register(student.getId(), period.getId());
    }

    protected List<Long> ids(SubjectEntity... subjects) {
        return Arrays.stream(subjects).map(SubjectEntity::getId).toList();
    }

    // The teacher responsible for each section grades the enrollment
    protected void grade(EnrollmentEntity enrollment, TeacherEntity teacher, double grade) {
        gradeService.registerGrade(enrollment.getId(), teacher.getId(), grade);
    }

    protected EnrollmentEntity enrollmentOf(List<EnrollmentEntity> enrollments, SubjectEntity subject) {
        return enrollments.stream().filter(e -> e.getSubjectId().equals(subject.getId())).findFirst().orElseThrow();
    }
}
