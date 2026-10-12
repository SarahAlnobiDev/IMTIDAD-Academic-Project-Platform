package com.imtidad.service;

import com.imtidad.contract.IReferenceDataService;
import com.imtidad.dao.ReferenceDataRepository;

import java.util.List;

/**
 * Service layer for reference data lookup.
 * Controllers must use this service rather than calling repositories directly.
 */
public class ReferenceDataService implements IReferenceDataService {

    private final ReferenceDataRepository referenceDataRepository;

    public ReferenceDataService() {
        this.referenceDataRepository = new ReferenceDataRepository();
    }

    @Override
    public List<String> getDepartments() {
        return referenceDataRepository.getDepartments();
    }

    @Override
    public List<String> getCourses() {
        return referenceDataRepository.getCourses();
    }

    @Override
    public List<String> getTechnologies() {
        return referenceDataRepository.getTechnologies();
    }

    /**
     * Generates a short list of recent academic-year options.
     *
     * @return list of academic-year strings such as "2024/2025"
     */
    @Override
    public List<String> getAcademicYearOptions() {
        int currentYear = java.time.Year.now().getValue();
        return List.of(
                (currentYear - 2) + "/" + (currentYear - 1),
                (currentYear - 1) + "/" + currentYear,
                currentYear + "/" + (currentYear + 1)
        );
    }
}
