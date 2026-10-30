package cl.ipt.siga.controller;

import cl.ipt.siga.entity.StudentEntity;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // GET /api/v1/students            -> all students
    // GET /api/v1/students?careerId=1 -> students of one career
    @GetMapping
    public ResponseEntity<List<StudentEntity>> getAll(@RequestParam(required = false) Long careerId) {
        if (careerId == null) {
            return ResponseEntity.ok(studentService.getAll());
        }
        return ResponseEntity.ok(studentService.getByCareer(careerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<StudentEntity> create(@RequestBody StudentEntity student) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(student));
    }

    // Updates personal data only (RUN, full name, email)
    @PutMapping("/{id}")
    public ResponseEntity<StudentEntity> update(@PathVariable Long id, @RequestBody StudentEntity student) {
        return ResponseEntity.ok(studentService.update(id, student));
    }

    // PUT /api/v1/students/1/status?status=POSTPONED
    // Only REGULAR, POSTPONED and TEMPORARY_WITHDRAWAL can be assigned manually
    @PutMapping("/{id}/status")
    public ResponseEntity<StudentEntity> changeStatus(@PathVariable Long id,
                                                      @RequestParam StudentStatus status) {
        return ResponseEntity.ok(studentService.changeStatus(id, status));
    }
}