package cl.ipt.siga.dto;

import java.util.List;

// Request body to create a section together with its schedule (e.g. ["L2", "W3"])
public record SectionRequest(
        Long subjectId,
        Long academicPeriodId,
        Long teacherId,
        int capacity,
        List<String> schedule
) { }