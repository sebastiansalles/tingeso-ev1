package cl.ipt.siga.controller;

import cl.ipt.siga.entity.SubjectEntity;
import cl.ipt.siga.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    // GET /api/v1/subjects               -> all subjects
    // GET /api/v1/subjects?studyPlanId=1 -> subjects of one study plan
    @GetMapping
    public ResponseEntity<List<SubjectEntity>> getAll(@RequestParam(required = false) Long studyPlanId) {
        if (studyPlanId == null) {
            return ResponseEntity.ok(subjectService.getAll());
        }
        return ResponseEntity.ok(subjectService.getByStudyPlan(studyPlanId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SubjectEntity> create(@RequestBody SubjectEntity subject) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(subject));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectEntity> update(@PathVariable Long id, @RequestBody SubjectEntity subject) {
        return ResponseEntity.ok(subjectService.update(id, subject));
    }
}