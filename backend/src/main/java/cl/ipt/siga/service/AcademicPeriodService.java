package cl.ipt.siga.service;

import cl.ipt.siga.entity.AcademicPeriodEntity;
import cl.ipt.siga.entity.enums.PeriodStatus;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AcademicPeriodService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final AcademicPeriodRepository academicPeriodRepository;

    public AcademicPeriodService(AcademicPeriodRepository academicPeriodRepository) {
        this.academicPeriodRepository = academicPeriodRepository;
    }

    public List<AcademicPeriodEntity> getAll() {
        return academicPeriodRepository.findAll();
    }

    public AcademicPeriodEntity getById(Long id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find an academic period with ID: " + id));
    }

    public AcademicPeriodEntity create(AcademicPeriodEntity period) {
        if (period.getSemester() != 1 && period.getSemester() != 2) {
            throw new BusinessRuleException("Semester must be 1 or 2");
        }
        if (period.getYear() < MIN_YEAR || period.getYear() > MAX_YEAR) {
            throw new BusinessRuleException("Year must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (academicPeriodRepository.existsByYearAndSemester(period.getYear(), period.getSemester())) {
            throw new DuplicateResourceException("Academic period " + period.getYear() + "-"
                    + period.getSemester() + " already exists");
        }

        period.setStatus(PeriodStatus.OPEN);   // initial status, never taken from the request
        period.setId(null);
        return academicPeriodRepository.save(period);
    }

    // Returns the period if it is OPEN; otherwise rejects the operation.
    // Used by sections, enrollments and grades.
    public AcademicPeriodEntity requireOpen(Long id) {
        AcademicPeriodEntity period = getById(id);   // 404
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("Academic period " + period.getYear() + "-"
                    + period.getSemester() + " is closed");
        }
        return period;
    }
}