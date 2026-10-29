package cl.ipt.siga.controller;

import cl.ipt.siga.entity.CareerEntity;
import cl.ipt.siga.service.CareerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/careers")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;

    @GetMapping("/{id}")
    public ResponseEntity<CareerEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(careerService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<CareerEntity>> getAll() {
        return ResponseEntity.ok(careerService.getAll());
    }

    @PostMapping
    public ResponseEntity<CareerEntity> create(@RequestBody CareerEntity careerEntity) {
        return ResponseEntity.ok(careerService.create(careerEntity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CareerEntity> update(@PathVariable Long id, @RequestBody CareerEntity careerEntity) {
        //return ResponseEntity.created(careerService.update(id, careerEntity));
        return ResponseEntity.status(HttpStatus.CREATED).body(careerService.update(id, careerEntity));
    }
}
