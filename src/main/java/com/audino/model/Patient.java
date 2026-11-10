package com.audino.model;

import com.audino.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Domain entity representing an outpatient receiving care at the clinic.
 *
 * // OOM: Encapsulation & Composition – encapsulates patient demographics and compositionally owns allergies and conditions.
 */
public class Patient {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    private Long id;
    private String patientId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String phone;
    private String address;
    private final List<PatientAllergy> allergies = new ArrayList<>();
    private final List<PatientCondition> conditions = new ArrayList<>();
    private final LocalDateTime createdAt;

    public Patient(Long id, String patientId, String firstName, String lastName,
                   LocalDate dateOfBirth, Gender gender, String phone, String address,
                   List<PatientAllergy> allergies, List<PatientCondition> conditions,
                   LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.firstName = Objects.requireNonNull(firstName, "First name must not be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name must not be null");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "Date of birth must not be null");
        this.gender = Objects.requireNonNull(gender, "Gender must not be null");
        setPhone(phone);
        this.address = address;
        if (allergies != null) {
            this.allergies.addAll(allergies);
        }
        if (conditions != null) {
            this.conditions.addAll(conditions);
        }
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Patient(String firstName, String lastName, LocalDate dateOfBirth, Gender gender, String phone, String address) {
        this(null, null, firstName, lastName, dateOfBirth, gender, phone, address, null, null, LocalDateTime.now());
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Dynamically calculates patient age in full years from date of birth.
     *
     * @return Age in years
     */
    public int getAge() {
        if (dateOfBirth == null) return 0;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    // OOM: Composition – Patient creates and owns its allergy records
    public void addAllergy(String allergen, String severity) {
        if (allergen != null && !allergen.isBlank()) {
            PatientAllergy allergy = new PatientAllergy(allergen.trim(), severity);
            allergy.setPatientId(this.id);
            this.allergies.add(allergy);
        }
    }

    // OOM: Composition – Patient creates and owns its chronic condition records
    public void addCondition(String conditionName, LocalDate diagnosedDate) {
        if (conditionName != null && !conditionName.isBlank()) {
            PatientCondition condition = new PatientCondition(conditionName.trim(), diagnosedDate);
            condition.setPatientId(this.id);
            this.conditions.add(condition);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = Objects.requireNonNull(firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = Objects.requireNonNull(lastName);
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth);
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = Objects.requireNonNull(gender);
    }

    public String getPhone() {
        return phone;
    }

    // OOM: Encapsulation – validating setter for phone number
    public void setPhone(String phone) {
        if (phone != null && !phone.isBlank()) {
            String clean = phone.trim().replaceAll("[^0-9]", "");
            if (clean.length() == 10 && PHONE_PATTERN.matcher(clean).matches()) {
                this.phone = clean;
                return;
            }
        }
        this.phone = phone != null ? phone.trim() : "";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // OOM: Encapsulation – returns unmodifiable defensive copy of collections
    public List<PatientAllergy> getAllergies() {
        return Collections.unmodifiableList(allergies);
    }

    public List<PatientCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient patient)) return false;
        return Objects.equals(patientId, patient.patientId) ||
               (Objects.equals(phone, patient.phone) && Objects.equals(firstName, patient.firstName) && Objects.equals(lastName, patient.lastName));
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId != null ? patientId : phone);
    }

    @Override
    public String toString() {
        return getFullName() + " (" + (patientId != null ? patientId : "New") + ", " + getAge() + "y)";
    }
}
