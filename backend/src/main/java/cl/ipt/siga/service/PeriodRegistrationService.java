package cl.ipt.siga.service;

import cl.ipt.siga.entity.AcademicPeriodEntity;
import cl.ipt.siga.entity.PeriodRegistrationEntity;
import cl.ipt.siga.entity.StudentEntity;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.repository.PeriodRegistrationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Registration ("matrícula") of a student in an academic period.
// The administrative process is outside SIGA IPT; the system only records the result,
// because enrollment requires the student to be registered in the period.
@Service
public class PeriodRegistrationService {

    private final PeriodRegistrationRepository periodRegistrationRepository;
    private final StudentService studentService;
    private final AcademicPeriodService academicPeriodService;

    public PeriodRegistrationService(PeriodRegistrationRepository periodRegistrationRepository,
                                     StudentService studentService,
                                     AcademicPeriodService academicPeriodService) {
        this.periodRegistrationRepository = periodRegistrationRepository;
        this.studentService = studentService;
        this.academicPeriodService = academicPeriodService;
    }

    public List<PeriodRegistrationEntity> getByAcademicPeriod(Long academicPeriodId) {
        academicPeriodService.getById(academicPeriodId);   // 404
        return periodRegistrationRepository.findByAcademicPeriodId(academicPeriodId);
    }

    public PeriodRegistrationEntity register(Long studentId, Long academicPeriodId) {
        StudentEntity student = studentService.getById(studentId);                          // 404
        AcademicPeriodEntity period = academicPeriodService.requireOpen(academicPeriodId);  // 404 / 400

        if (isRegistered(studentId, academicPeriodId)) {
            throw new DuplicateResourceException("Student " + student.getRun() + " is already registered in period "
                    + period.getYear() + "-" + period.getSemester());
        }

        PeriodRegistrationEntity registration = new PeriodRegistrationEntity();
        registration.setStudentId(studentId);
        registration.setAcademicPeriodId(academicPeriodId);
        return periodRegistrationRepository.save(registration);
    }

    // Used by the enrollment process
    public boolean isRegistered(Long studentId, Long academicPeriodId) {
        return periodRegistrationRepository.existsByStudentIdAndAcademicPeriodId(studentId, academicPeriodId);
    }
}