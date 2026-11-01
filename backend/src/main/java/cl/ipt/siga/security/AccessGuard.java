package cl.ipt.siga.security;

import cl.ipt.siga.repository.EnrollmentRepository;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.repository.StudentRepository;
import cl.ipt.siga.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Ownership checks used from @PreAuthorize as "@access.method(...)".
// The role says WHAT a user may do; these checks say ON WHICH DATA.
// They return false (-> 403) instead of throwing, so a user cannot probe which ids exist.
@Component("access")
@RequiredArgsConstructor
public class AccessGuard {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public boolean isStudent(Long studentId) {
        String email = CurrentUser.email();
        return email != null && studentRepository.findById(studentId)
                .map(student -> email.equals(student.getEmail()))
                .orElse(false);
    }

    public boolean isTeacher(Long teacherId) {
        String email = CurrentUser.email();
        return email != null && teacherRepository.findById(teacherId)
                .map(teacher -> email.equals(teacher.getEmail()))
                .orElse(false);
    }

    public boolean isTeacherOfSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .map(section -> isTeacher(section.getTeacherId()))
                .orElse(false);
    }

    public boolean isOwnEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .map(enrollment -> isStudent(enrollment.getStudentId()))
                .orElse(false);
    }
}