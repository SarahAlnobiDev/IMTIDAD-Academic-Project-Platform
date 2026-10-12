package com.imtidad.contract;

import java.util.List;

/**
 * Service contract for dynamic reference data lookup.
 */
public interface IReferenceDataService {

    List<String> getDepartments();

    List<String> getCourses();

    List<String> getTechnologies();

    List<String> getAcademicYearOptions();
}
