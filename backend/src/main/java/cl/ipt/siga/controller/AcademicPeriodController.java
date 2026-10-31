package cl.ipt.siga.controller;

import cl.ipt.siga.entity.AcademicPeriodEntity;
import cl.ipt.siga.service.AcademicPeriodService;
import cl.ipt.siga.service.PeriodClosingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-periods")
@RequiredArgsConstructor
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;
    private final PeriodClosingService periodClosingService;

    // GET /api/v1/academic-periods -> 200 OK
    @GetMapping
    public ResponseEntity<List<AcademicPeriodEntity>> getAll(){
        return ResponseEntity.ok(academicPeriodService.getAll());
    }

    // GET /api/v1/academic-periods/{id} -> 200 OK
    @GetMapping("/{id}")
    public ResponseEntity<AcademicPeriodEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(academicPeriodService.getById(id));
    }

    // POST /api/v1/academic-periods -> 201 CREATED
    @PostMapping
    public ResponseEntity<AcademicPeriodEntity> create(@RequestBody AcademicPeriodEntity academicPeriod) {
        AcademicPeriodEntity createdPeriod = academicPeriodService.create(academicPeriod);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPeriod);
    }




    // POST /api/v1/academic-periods/1/close -> runs the closing process
    @PostMapping("/{id}/close")
    public ResponseEntity<AcademicPeriodEntity> close(@PathVariable Long id) {
        return ResponseEntity.ok(periodClosingService.close(id));
    }
}