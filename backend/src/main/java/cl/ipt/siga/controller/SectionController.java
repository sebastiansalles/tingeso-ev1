package cl.ipt.siga.controller;

import cl.ipt.siga.dto.SectionRequest;
import cl.ipt.siga.entity.ScheduleBlockEntity;
import cl.ipt.siga.entity.SectionEntity;
import cl.ipt.siga.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    // GET /api/v1/sections                    -> all sections
    // GET /api/v1/sections?academicPeriodId=1 -> academic offer of one period
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<SectionEntity>> getAll(@RequestParam(required = false) Long academicPeriodId) {
        if (academicPeriodId == null) {
            return ResponseEntity.ok(sectionService.getAll());
        }
        return ResponseEntity.ok(sectionService.getByAcademicPeriod(academicPeriodId));
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<SectionEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sectionService.getById(id));
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/schedule")
    public ResponseEntity<List<ScheduleBlockEntity>> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(sectionService.getSchedule(id));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SectionEntity> create(@RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.create(request));
    }
}