package cl.ipt.siga.controller;

import cl.ipt.siga.entity.EnrollmentEntity;
import cl.ipt.siga.entity.SubjectEntity;
import cl.ipt.siga.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // GET /api/v1/students/1/available-subjects?academicPeriodId=1
    @GetMapping("/students/{studentId}/available-subjects")
    public ResponseEntity<List<SubjectEntity>> getAvailableSubjects(@PathVariable Long studentId,
                                                                    @RequestParam Long academicPeriodId) {
        return ResponseEntity.ok(enrollmentService.getAvailableSubjects(studentId, academicPeriodId));
    }

    // POST /api/v1/students/1/enrollments?academicPeriodId=1   body: [1, 2, 3]  (subject ids)
    @PostMapping("/students/{studentId}/enrollments")
    public ResponseEntity<List<EnrollmentEntity>> enroll(@PathVariable Long studentId,
                                                         @RequestParam Long academicPeriodId,
                                                         @RequestBody List<Long> subjectIds) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(studentId, academicPeriodId, subjectIds));
    }

    // GET /api/v1/students/1/enrollments                       -> all
    // GET /api/v1/students/1/enrollments?academicPeriodId=1    -> one period
    @GetMapping("/students/{studentId}/enrollments")
    public ResponseEntity<List<EnrollmentEntity>> getByStudent(@PathVariable Long studentId,
                                                               @RequestParam(required = false) Long academicPeriodId) {
        return ResponseEntity.ok(enrollmentService.getByStudent(studentId, academicPeriodId));
    }

    // GET /api/v1/students/1/academic-history
    @GetMapping("/students/{studentId}/academic-history")
    public ResponseEntity<List<EnrollmentEntity>> getAcademicHistory(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getAcademicHistory(studentId));
    }

    // DELETE /api/v1/enrollments/7 -> cancels enrollment 7 and frees its seat
    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        enrollmentService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}