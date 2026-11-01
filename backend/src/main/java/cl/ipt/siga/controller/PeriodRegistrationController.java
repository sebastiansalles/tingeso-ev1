package cl.ipt.siga.controller;

import cl.ipt.siga.entity.PeriodRegistrationEntity;
import cl.ipt.siga.service.PeriodRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-periods/{academicPeriodId}/registrations")
@RequiredArgsConstructor
public class PeriodRegistrationController {

    private final PeriodRegistrationService periodRegistrationService;

    // GET /api/v1/academic-periods/1/registrations -> students registered in period 1
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PeriodRegistrationEntity>> getAll(@PathVariable Long academicPeriodId) {
        return ResponseEntity.ok(periodRegistrationService.getByAcademicPeriod(academicPeriodId));
    }

    // POST /api/v1/academic-periods/1/registrations/5 -> registers student 5 in period 1
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{studentId}")
    public ResponseEntity<PeriodRegistrationEntity> register(@PathVariable Long academicPeriodId,
                                                             @PathVariable Long studentId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(periodRegistrationService.register(studentId, academicPeriodId));
    }
}