package cl.ipt.siga.controller;


import cl.ipt.siga.entity.PrerequisiteEntity;
import cl.ipt.siga.service.PrerequisiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/prerequisites")
@RequiredArgsConstructor
public class PrerequisiteController {

    private final PrerequisiteService prerequisiteService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<PrerequisiteEntity>> getAll(@PathVariable Long subjectId) {
        return ResponseEntity.ok(prerequisiteService.getPrerequisites(subjectId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{prerequisiteId}")
    public ResponseEntity<PrerequisiteEntity> add(@PathVariable Long subjectId,
                                                  @PathVariable Long prerequisiteId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prerequisiteService.addPrerequisite(subjectId, prerequisiteId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{prerequisiteId}")
    public ResponseEntity<Void> remove(@PathVariable Long subjectId,
                                       @PathVariable Long prerequisiteId) {
        prerequisiteService.removePrerequisite(subjectId, prerequisiteId);
        return ResponseEntity.noContent().build();
    }
}