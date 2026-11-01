package cl.ipt.siga.controller;

import cl.ipt.siga.entity.TeacherEntity;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<TeacherEntity>> getAll() {
        return ResponseEntity.ok(teacherService.getAll());
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<TeacherEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getById(id));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TeacherEntity> create(@RequestBody TeacherEntity teacher) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.create(teacher));
    }

    // Updates personal and academic data (RUN, name, email, title, degree)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TeacherEntity> update(@PathVariable Long id, @RequestBody TeacherEntity teacher) {
        return ResponseEntity.ok(teacherService.update(id, teacher));
    }

    // PUT /api/v1/teachers/1/status?status=INACTIVE
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<TeacherEntity> changeStatus(@PathVariable Long id,
                                                      @RequestParam TeacherStatus status) {
        return ResponseEntity.ok(teacherService.changeStatus(id, status));
    }
}