package cl.ipt.siga.controller;

import cl.ipt.siga.entity.StudyPlanEntity;
import cl.ipt.siga.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;


    @GetMapping
    public ResponseEntity<List<StudyPlanEntity>> getAll(@RequestParam(required = false) Long careerId) {
        if (careerId == null) {
            // all plans
            return ResponseEntity.ok(studyPlanService.getAll());
        }
        //plans of the provided careerID
        return ResponseEntity.ok(studyPlanService.getByCareer(careerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyPlanEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studyPlanService.getById(id));
    }

    @PostMapping
    public ResponseEntity<StudyPlanEntity> create(@RequestBody StudyPlanEntity studyPlan) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studyPlanService.create(studyPlan));
    }
}
