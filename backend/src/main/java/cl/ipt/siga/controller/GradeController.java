package cl.ipt.siga.controller;

import cl.ipt.siga.entity.EnrollmentEntity;
import cl.ipt.siga.entity.SectionEntity;
import cl.ipt.siga.service.GradeService;
import cl.ipt.siga.service.SectionService;
import cl.ipt.siga.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import cl.ipt.siga.security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final SectionService sectionService;
    private final TeacherService teacherService;

    // GET /api/v1/teachers/1/sections?academicPeriodId=1 -> sections assigned to the teacher
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and @access.isTeacher(#teacherId))")
    @GetMapping("/teachers/{teacherId}/sections")
    public ResponseEntity<List<SectionEntity>> getTeacherSections(@PathVariable Long teacherId,
                                                                  @RequestParam(required = false) Long academicPeriodId) {
        return ResponseEntity.ok(sectionService.getByTeacher(teacherId, academicPeriodId));
    }

    // GET /api/v1/sections/1/enrollments -> students enrolled in the section
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and @access.isTeacherOfSection(#sectionId))")
    @GetMapping("/sections/{sectionId}/enrollments")
    public ResponseEntity<List<EnrollmentEntity>> getSectionEnrollments(@PathVariable Long sectionId) {
        return ResponseEntity.ok(gradeService.getSectionEnrollments(sectionId));
    }

    // PUT /api/v1/enrollments/7/grade?grade=5.5
    // The teacher is the logged-in user (identified by the email in the Keycloak token)
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/enrollments/{enrollmentId}/grade")
    public ResponseEntity<EnrollmentEntity> registerGrade(@PathVariable Long enrollmentId,
                                                          @RequestParam Double grade) {
        Long teacherId = teacherService.getByEmail(CurrentUser.email()).getId();
        return ResponseEntity.ok(gradeService.registerGrade(enrollmentId, teacherId, grade));
    }
}